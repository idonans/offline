package com.idonans.offline;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.idonans.acommon.App;
import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.Threads;
import com.idonans.acommon.lang.WeakAvailable;
import com.idonans.acommon.util.ViewUtil;

/**
 * 启动页
 * Created by idonans on 16-4-27.
 */
public class SplashActivity extends CommonActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        TextView versionDesc = ViewUtil.findViewByID(this, R.id.version_desc);
        versionDesc.setText("v" + App.getBuildConfigAdapter().getVersionName());

        Threads.postBackground(new DelayRedirectTask(this));
    }

    private void direct() {
        CommonLog.d(TAG + " direct");
        Intent intent = new Intent(this, FunctionsActivity.class);
        startActivity(intent);
        finish();
    }

    private static class DelayRedirectTask implements Runnable {

        private final WeakAvailable mSplashActivityAvailable;

        private DelayRedirectTask(SplashActivity splashActivity) {
            mSplashActivityAvailable = new WeakAvailable(splashActivity);
        }

        @Override
        public void run() {
            CommonLog.d(TAG + " try sleep 3000 ms");
            Threads.sleepQuietly(3000);

            if (mSplashActivityAvailable.isAvailable()) {
                CommonLog.d(TAG + " post to ui direct");
                Threads.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        SplashActivity splashActivity = (SplashActivity) mSplashActivityAvailable.getObject();
                        if (mSplashActivityAvailable.isAvailable()) {
                            splashActivity.direct();
                        }
                    }
                });
            }
        }

    }

}
