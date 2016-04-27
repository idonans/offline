package com.idonans.offline.data;

import com.idonans.acommon.App;
import com.idonans.acommon.lang.CommonLog;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * okhttp
 * Created by idonans on 16-4-27.
 */
public class HttpManager {

    private static class InstanceHolder {

        private static final HttpManager sInstance = new HttpManager();

    }

    public static HttpManager getInstance() {
        return InstanceHolder.sInstance;
    }

    private static final String TAG = "HttpManager";
    private final OkHttpClient mOkHttpClient;

    private HttpManager() {
        if (App.getBuildConfigAdapter().isDebug()) {
            CommonLog.d(TAG + " in debug mode, config OkHttpClient.");

            Interceptor contentEncodingInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    CommonLog.d(TAG + " contentEncodingInterceptor intercept");
                    Request request = chain.request().newBuilder().removeHeader("Accept-Encoding")
                            .addHeader("Accept-Encoding", "identity").build();
                    return chain.proceed(request);
                }
            };

            HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            mOkHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(contentEncodingInterceptor)
                    .addInterceptor(httpLoggingInterceptor)
                    .build();
        } else {
            mOkHttpClient = new OkHttpClient.Builder()
                    .build();
        }

    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

}
