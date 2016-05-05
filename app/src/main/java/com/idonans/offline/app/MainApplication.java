package com.idonans.offline.app;

import android.app.Application;

import com.idonans.acommon.App;
import com.idonans.offline.fresco.FrescoManager;
import com.squareup.leakcanary.LeakCanary;

/**
 * init
 * Created by idonans on 16-4-27.
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
        App.init(this, new BuildConfigAdapterImpl());
        FrescoManager.getInstance();
    }

}
