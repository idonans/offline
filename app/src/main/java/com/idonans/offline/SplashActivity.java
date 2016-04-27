package com.idonans.offline;

import android.os.Bundle;
import android.widget.TextView;

import com.idonans.acommon.App;
import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.util.ViewUtil;

/**
 * 启动页
 * Created by idonans on 16-4-27.
 */
public class SplashActivity extends CommonActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        TextView versionDesc = ViewUtil.findViewByID(this, R.id.version_desc);
        versionDesc.setText("v" + App.getBuildConfigAdapter().getVersionName());
    }

}
