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

    public AuthHolder sso(Activity activity, AuthCallback authCallback) {
        SsoHandler ssoHandler = new SsoHandler(activity, mAuthInfo);
        return new AuthHolder(ssoHandler, authCallback);
    }

    public class AuthHolder {
        private final SsoHandler mSsoHandler;
        private final WeiboAuthListener mWeiboAuthListener;

        private AuthHolder(SsoHandler ssoHandler, AuthCallback authCallback) {
            mSsoHandler = ssoHandler;
            mWeiboAuthListener = new AuthCallbackAdapter(authCallback);
        }

        public void auth() {
            if (mSsoHandler != null) {
                mSsoHandler.authorize(mWeiboAuthListener);
            }
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (mSsoHandler != null) {
                mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
            }
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
