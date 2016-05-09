package com.idonans.offline.share.sinaweibo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Toast;

import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.offline.R;
import com.idonans.sharesinaweibo.ShareSinaWeibo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;

/**
 * 新浪微博登陆分享
 * Created by idonans on 16-5-9.
 */
public class ShareSinaWeiboActivity extends CommonActivity {

    private static final String TAG = "ShareSinaWeiboActivity";
    private static final String APP_KEY = "750726543";

    private ShareSinaWeibo.AuthHolder mAuthHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_sina_weibo_activity);

        mAuthHolder = new ShareSinaWeibo(APP_KEY).sso(this, new ShareSinaWeibo.AuthCallback() {
            @Override
            public void onSuccess(@NonNull Oauth2AccessToken accessToken) {
                CommonLog.d(TAG + " onSuccess " + accessToken.getToken());
                showToast("登陆成功 token:" + accessToken.getToken());
            }

            @Override
            public void onCancel() {
                CommonLog.d(TAG + " onCancel");
                showToast("已取消登陆");
            }

            @Override
            public void onError(Exception e) {
                CommonLog.d(TAG + " onError");
                e.printStackTrace();
                showToast("登录失败");
            }
        });

        View sinaWeiboSso = ViewUtil.findViewByID(this, R.id.sina_weibo_login);
        sinaWeiboSso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuthHolder.auth();
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mAuthHolder.onActivityResult(requestCode, resultCode, data);
    }

}
