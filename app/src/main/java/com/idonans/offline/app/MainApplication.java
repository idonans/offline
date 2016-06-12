package com.idonans.offline.app;

import android.app.Application;

import com.idonans.acommon.App;
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
        App.init(new App.Config()
                .setContext(this)
                .setBuildConfigAdapter(new BuildConfigAdapterImpl()));
    }

}
