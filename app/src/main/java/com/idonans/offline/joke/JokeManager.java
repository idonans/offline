package com.idonans.offline.joke;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.idonans.acommon.data.StorageManager;
import com.idonans.acommon.lang.Available;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.offline.data.HttpManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

/**
 * 笑话大全
 * Created by idonans on 16-4-27.
 */
public class JokeManager {

    private static class InstanceHolder {

        private static final JokeManager sInstance = new JokeManager();

    }

    public static JokeManager getInstance() {
        return InstanceHolder.sInstance;
    }

    private static final String TAG = "JokeManager";
    private final JokeApiService mJokeApiService;

    private static final String KEY_JOKE_OFFLINE_INFO = "offline_joke_offline_info";
    private JokeOfflineInfo mJokeOfflineInfo;

    // 如果当前正在缓存新的笑话，用来临时记录一个缓存 key, 当新的笑话缓存成功时，此 key 会被写入 JokeOfflineInfo
    private String mContentKeyLoading;

    // 如果是 null, 则标示没有从磁盘中加载到内存, 如果是 empty 集合，则标示磁盘中没有缓存
    private List<Data.Joke> mOfflineJokes;

    private JokeManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://japi.juhe.cn/")
                .client(HttpManager.getInstance().getOkHttpClient())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mJokeApiService = retrofit.create(JokeApiService.class);
        mJokeOfflineInfo = restoreJokeOfflineInfo();
    }

    /**
     * @param force 是否强制开始新的缓存
     */
    public void offline(boolean force) {
        if (force) {
            startOffline();
            return;
        }

        if (isLoading()) {
            // 正在缓存中，不必开始新的缓存
            return;
        }

        if (mJokeOfflineInfo != null
                && mJokeOfflineInfo.hasContent()
                && !mJokeOfflineInfo.isTimeout()) {
            // 缓存有内容并且没有过期，则不必开始新的缓存
            return;
        }

        startOffline();
    }

    public long getOfflineJokesTime() {
        if (mJokeOfflineInfo != null && mJokeOfflineInfo.hasContent()) {
            return mJokeOfflineInfo.offlineTime;
        }
        return 0;
    }

    public List<Data.Joke> getOfflineJokes() {
        if (mJokeOfflineInfo != null && mJokeOfflineInfo.hasContent()) {
            if (mOfflineJokes != null) {
                return mOfflineJokes;
            }

            List<Data.Joke> offlineJokes = restoreOfflineJokes(mJokeOfflineInfo.contentKey);
            if (offlineJokes == null) {
                offlineJokes = new ArrayList<>();
            }
            mOfflineJokes = offlineJokes;
        }
        return mOfflineJokes;
    }

    /**
     * 是否正在缓存新的内容
     */
    public boolean isLoading() {
        if (TextUtils.isEmpty(mContentKeyLoading)) {
            return false;
        }
        if (mJokeOfflineInfo != null && mContentKeyLoading.equals(mJokeOfflineInfo.contentKey)) {
            return false;
        }
        return true;
    }

    private long getTime10Bit() {
        // PHP 10 位时间戳
        return Long.valueOf(String.valueOf(System.currentTimeMillis()).substring(0, 10));
    }

    /**
     * 开始缓存新的内容
     */
    private void startOffline() {
        final String contentKeyLoading = UUID.randomUUID().toString();
        CommonLog.d(TAG + " startOffline " + contentKeyLoading);

        mContentKeyLoading = contentKeyLoading;
        final Available finalAvailable = new Available() {
            @Override
            public boolean isAvailable() {
                return contentKeyLoading.equals(mContentKeyLoading);
            }
        };
        mJokeApiService.getLastestJokes(getTime10Bit())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<Data>() {
                    @Override
                    public void onCompleted() {
                        boolean available = finalAvailable.isAvailable();
                        CommonLog.d(TAG + " offline onCompleted " + contentKeyLoading + ", " + available);
                    }

                    @Override
                    public void onError(Throwable e) {
                        boolean available = finalAvailable.isAvailable();
                        CommonLog.d(TAG + " offline onError " + contentKeyLoading + ", " + available);
                        if (!available) {
                            return;
                        }
                        mContentKeyLoading = null;
                    }

                    @Override
                    public void onNext(Data data) {
                        boolean available = finalAvailable.isAvailable();
                        CommonLog.d(TAG + " offline onNext " + contentKeyLoading + ", " + available);
                        if (!available) {
                            return;
                        }

                        if (data == null || data.error_code != 0) {
                            mContentKeyLoading = null;
                            return;
                        }

                        if (data.result != null && data.result.data != null && !data.result.data.isEmpty()) {
                            // save joke
                            JokeOfflineInfo jokeOfflineInfo = saveJokeOffline(mContentKeyLoading, data.result.data);
                            if (jokeOfflineInfo != null) {
                                // 新的缓存成功保存，同步到内存
                                mJokeOfflineInfo = jokeOfflineInfo;
                                mOfflineJokes = data.result.data;
                            }
                        }
                        mContentKeyLoading = null;
                    }
                });
    }

    /**
     * 从磁盘中读取缓存的笑话信息
     */
    private static JokeOfflineInfo restoreJokeOfflineInfo() {
        try {
            String json = StorageManager.getInstance().getCache(KEY_JOKE_OFFLINE_INFO);
            if (!TextUtils.isEmpty(json)) {
                Gson gson = new Gson();
                Type type = new TypeToken<JokeOfflineInfo>() {
                }.getType();
                JokeOfflineInfo jokeOfflineInfo = gson.fromJson(json, type);
                if (jokeOfflineInfo != null && jokeOfflineInfo.hasContent()) {
                    return jokeOfflineInfo;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 保存笑话信息到磁盘
     */
    private static boolean saveJokeOfflineInfo(JokeOfflineInfo jokeOfflineInfo) {
        if (jokeOfflineInfo != null && jokeOfflineInfo.hasContent()) {
            Gson gson = new Gson();
            Type type = new TypeToken<JokeOfflineInfo>() {
            }.getType();
            String json = gson.toJson(jokeOfflineInfo, type);
            StorageManager.getInstance().setCache(KEY_JOKE_OFFLINE_INFO, json);
            return true;
        }
        return false;
    }

    private static JokeOfflineInfo saveJokeOffline(String key, List<Data.Joke> jokes) {
        if (TextUtils.isEmpty(key)) {
            key = UUID.randomUUID().toString();
        }
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Data.Joke>>() {
            }.getType();
            String json = gson.toJson(jokes, type);
            if (!TextUtils.isEmpty(json)) {
                StorageManager.getInstance().setCache(key, json);

                JokeOfflineInfo jokeOfflineInfo = new JokeOfflineInfo();
                jokeOfflineInfo.contentKey = key;
                jokeOfflineInfo.offlineTime = System.currentTimeMillis();
                if (saveJokeOfflineInfo(jokeOfflineInfo)) {
                    return jokeOfflineInfo;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<Data.Joke> restoreOfflineJokes(String key) {
        try {
            String json = StorageManager.getInstance().getCache(key);
            if (!TextUtils.isEmpty(json)) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<Data.Joke>>() {
                }.getType();
                List<Data.Joke> offlineJokes = gson.fromJson(json, type);
                return offlineJokes;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class JokeOfflineInfo {
        public long offlineTime;
        // 笑话内容比较长，肯能不适宜在启动式直接加载到内存，使用 contentKey 来指向笑话内容在磁盘缓存中的 key
        public String contentKey;

        /**
         * 上一次缓存的笑话内容是否正确, 不关心当前是否正在缓存新的笑话内容
         */
        public boolean hasContent() {
            return !TextUtils.isEmpty(contentKey);
        }

        /**
         * 上一次缓存的笑话是否已经过期
         */
        public boolean isTimeout() {
            return System.currentTimeMillis() - offlineTime > TimeUnit.DAYS.toMillis(1);
        }
    }

    public interface JokeApiService {

        // http://japi.juhe.cn/joke/content/list.from?key=您申请的KEY&page=2&pagesize=10&sort=asc&time=1418745237
        @GET("/joke/content/list.from?key=ac043f2d37f9b8cdadfbe16257d1c72e&page=1&pagesize=500&sort=desc")
        Observable<Data> getLastestJokes(@Query("time") long time);

    }

    public static class Data {
        public int error_code;
        public String reason;
        public Result result;

        public static class Result {
            public List<Joke> data;
        }

        public static class Joke {
            public String content;
            public String hashId;
            public long unixtime;
            public String updatetime;
        }
    }

}
