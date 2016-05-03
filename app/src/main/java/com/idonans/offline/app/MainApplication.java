package com.idonans.offline.app;

import android.app.Application;

import com.idonans.acommon.App;
import com.idonans.offline.fresco.FrescoManager;

/**
 * init
 * Created by idonans on 16-4-27.
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        App.init(this, new BuildConfigAdapterImpl());
        FrescoManager.getInstance();
    }

}
