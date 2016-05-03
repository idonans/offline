package com.idonans.offline.fresco;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.common.logging.FLog;
import com.facebook.common.logging.FLogDefaultLoggingDelegate;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpNetworkFetcher;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.idonans.acommon.App;
import com.idonans.acommon.AppContext;
import com.idonans.acommon.data.ProcessManager;
import com.idonans.offline.data.HttpManager;

/**
 * fresco 图片加载
 * Created by idonans on 16-5-3.
 */
public class FrescoManager {

    private static class InstanceHolder {

        private static final FrescoManager sInstance = new FrescoManager();

    }

    public static FrescoManager getInstance() {
        return InstanceHolder.sInstance;
    }

    private FrescoManager() {
        ImagePipelineConfig imagePipelineConfig = ImagePipelineConfig.newBuilder(AppContext.getContext())
                .setMainDiskCacheConfig(DiskCacheConfig.newBuilder(AppContext.getContext())
                        .setBaseDirectoryName("fresco_main_disk_" + ProcessManager.getInstance().getProcessTag())
                        .build())
                .setSmallImageDiskCacheConfig(DiskCacheConfig.newBuilder(AppContext.getContext())
                        .setBaseDirectoryName("fresco_small_disk_" + ProcessManager.getInstance().getProcessTag())
                        .build())
                .setNetworkFetcher(new OkHttpNetworkFetcher(HttpManager.getInstance().getOkHttpClient()))
                .build();

        FLogDefaultLoggingDelegate fLogDefaultLoggingDelegate = FLogDefaultLoggingDelegate.getInstance();
        fLogDefaultLoggingDelegate.setApplicationTag(App.getBuildConfigAdapter().getLogTag());
        if (App.getBuildConfigAdapter().isDebug()) {
            fLogDefaultLoggingDelegate.setMinimumLoggingLevel(FLog.DEBUG);
        }
        Fresco.initialize(AppContext.getContext(), imagePipelineConfig);
    }

}
