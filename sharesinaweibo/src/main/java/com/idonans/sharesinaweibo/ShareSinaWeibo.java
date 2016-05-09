package com.idonans.sharesinaweibo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.idonans.acommon.AppContext;
import com.idonans.acommon.lang.CommonLog;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;

import java.lang.reflect.Field;

/**
 * 新浪微博 分享
 * Created by idonans on 16-5-9.
 */
public class ShareSinaWeibo {

    private static final String TAG = "ShareSinaWeibo";
    private final String mAppKey;
    private final String mRedirectUrl = "https://api.weibo.com/oauth2/default.html";
    private final String mScope = "";

    private final AuthInfo mAuthInfo;

    public ShareSinaWeibo(String appKey) {
        this.mAppKey = appKey;
        mAuthInfo = new AuthInfo(AppContext.getContext(), mAppKey, mRedirectUrl, mScope);
    }

    public AuthHolder sso(Activity activity, Bundle savedInstanceState, AuthCallback authCallback) {
        SsoHandler ssoHandler = new SsoHandler(activity, mAuthInfo);
        return new AuthHolder(savedInstanceState, ssoHandler, authCallback);
    }

    public class AuthHolder {
        private static final String EXTRA_REQUEST_CODE_SINA_WEIBO_SSO_AUTH = "extra.REQUEST_CODE_SINA_WEIBO_SSO_AUTH";
        private final SsoHandler mSsoHandler;
        private final WeiboAuthListener mWeiboAuthListener;

        private AuthHolder(Bundle savedInstanceState, SsoHandler ssoHandler, AuthCallback authCallback) {
            CommonLog.d(TAG + " init AuthHolder savedInstanceState " + ((savedInstanceState == null ? "not found" : "found")));
            mSsoHandler = ssoHandler;
            mWeiboAuthListener = new AuthCallbackAdapter(authCallback);

            // TODO 新浪微博 SDK 实现中对进程被回收后的状态恢复处理不足，此处虽然尝试恢复中间数据，但是对网页授权方式没有实际效果。 对 SSO 微博客户端授权有一定的修复效果。
            // 恢复 mSSOAuthRequestCode 和 mAuthListener (如果当前进程被回收)
            if (savedInstanceState != null) {
                int code = savedInstanceState.getInt(EXTRA_REQUEST_CODE_SINA_WEIBO_SSO_AUTH, -1);
                if (code != -1) {
                    try {
                        Field fRequestCode = SsoHandler.class.getDeclaredField("mSSOAuthRequestCode");
                        fRequestCode.setAccessible(true);
                        fRequestCode.setInt(mSsoHandler, code);
                        CommonLog.d(TAG + " restore SsoHandler mSSOAuthRequestCode " + code);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Field fAuthListener = SsoHandler.class.getDeclaredField("mAuthListener");
                        fAuthListener.setAccessible(true);
                        fAuthListener.set(mSsoHandler, mWeiboAuthListener);
                        CommonLog.d(TAG + " restore SsoHandler mAuthListener");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        public void auth() {
            CommonLog.d(TAG + " AuthHolder auth");
            mSsoHandler.authorize(mWeiboAuthListener);
        }

        public void onSaveInstanceState(Bundle outState) {
            CommonLog.d(TAG + " AuthHolder onSaveInstanceState");
            try {
                Field fRequestCode = SsoHandler.class.getDeclaredField("mSSOAuthRequestCode");
                fRequestCode.setAccessible(true);
                int code = fRequestCode.getInt(mSsoHandler);
                outState.putInt(EXTRA_REQUEST_CODE_SINA_WEIBO_SSO_AUTH, code);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            CommonLog.d(TAG + " AuthHolder onActivityResult requestCode " + requestCode + ", resultCode " + resultCode);
            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
        }

    }

    private class AuthCallbackAdapter implements WeiboAuthListener {

        private final AuthCallback mAuthCallback;

        private AuthCallbackAdapter(AuthCallback authCallback) {
            if (authCallback == null) {
                authCallback = new SimpleAuthCallback();
            }
            this.mAuthCallback = authCallback;
        }

        @Override
        public void onComplete(Bundle bundle) {
            Oauth2AccessToken accessToken = Oauth2AccessToken.parseAccessToken(bundle);
            if (accessToken != null && accessToken.isSessionValid()) {
                this.mAuthCallback.onSuccess(accessToken);
            } else {
                String code = null;
                if (bundle != null) {
                    // 当应用签名不正确时，会收到 code 值
                    code = bundle.getString("code", null);
                }
                Exception exception = new RuntimeException("code " + code);
                this.mAuthCallback.onError(exception);
            }
        }

        @Override
        public void onWeiboException(WeiboException e) {
            this.mAuthCallback.onError(e);
        }

        @Override
        public void onCancel() {
            this.mAuthCallback.onCancel();
        }
    }

    public interface AuthCallback {
        void onSuccess(@NonNull Oauth2AccessToken accessToken);

        void onCancel();

        void onError(Exception e);
    }

    private class SimpleAuthCallback implements AuthCallback {

        @Override
        public void onSuccess(@NonNull Oauth2AccessToken accessToken) {
            CommonLog.d(TAG + " onSuccess SimpleAuthCallback");
        }

        @Override
        public void onCancel() {
            CommonLog.d(TAG + " onCancel SimpleAuthCallback");
        }

        @Override
        public void onError(Exception e) {
            CommonLog.d(TAG + " onError SimpleAuthCallback");
            e.printStackTrace();
        }
    }

}
