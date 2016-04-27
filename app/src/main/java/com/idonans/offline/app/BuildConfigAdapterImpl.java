package com.idonans.offline.app;

import android.util.Log;

import com.idonans.acommon.App;
import com.idonans.offline.BuildConfig;

/**
 * build config impl
 * Created by idonans on 16-4-27.
 */
public class BuildConfigAdapterImpl implements App.BuildConfigAdapter {

    @Override
    public int getVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public String getVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public String getLogTag() {
        return BuildConfig.APPLICATION_ID;
    }

    @Override
    public int getLogLevel() {
        return Log.DEBUG;
    }

    @Override
    public boolean isDebug() {
        return BuildConfig.DEBUG;
    }

}
