package com.idonans.offline.app;

import android.app.Application;

import com.idonans.acommon.App;

/**
 * init
 * Created by idonans on 16-4-27.
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        App.init(this, new BuildConfigAdapterImpl());
    }

}
