package com.idonans.offline.joke;

import android.support.annotation.CheckResult;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.idonans.acommon.data.StorageManager;
import com.idonans.acommon.lang.Available;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.NotAvailableException;
import com.idonans.offline.data.HttpManager;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
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
    // 如果当前正在缓存新的笑话，此值用来记录已经缓存的页码
    private int mLoadedPagesCount;

    private WeakReference<List<Data.Joke>> mOfflineJokesRef;

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

        startOffline();
    }

    /**
     * 如果正在缓存，则取消
     */
    public void cancel() {
        mContentKeyLoading = null;
    }

    /**
     * 上一次缓存成功的时间 ms
     */
    public long getOfflineJokesTime() {
        if (mJokeOfflineInfo != null && mJokeOfflineInfo.hasContent()) {
            return mJokeOfflineInfo.offlineTime;
        }
        return 0;
    }

    public Observable<List<Data.Joke>> getOfflineJokes() {
        return Observable.create(new Observable.OnSubscribe<List<Data.Joke>>() {
            @Override
            public void call(Subscriber<? super List<Data.Joke>> subscriber) {
                List<Data.Joke> offlineJokes = loadOfflineJokesToMemory();
                subscriber.onNext(offlineJokes);
                subscriber.onCompleted();
            }
        });
    }

    private List<Data.Joke> loadOfflineJokesToMemory() {
        if (mOfflineJokesRef != null) {
            List<Data.Joke> offlineJokes = mOfflineJokesRef.get();
            if (offlineJokes != null) {
                return offlineJokes;
            }
        }
        List<Data.Joke> offlineJokes = null;
        if (mJokeOfflineInfo != null && mJokeOfflineInfo.hasContent()) {
            offlineJokes = restoreOfflineJokes(mJokeOfflineInfo.contentKey);
        }
        if (offlineJokes == null) {
            offlineJokes = new ArrayList<>();
        }
        mOfflineJokesRef = new WeakReference<>(offlineJokes);
        return offlineJokes;
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

    public float getLoadingProgress() {
        if (!isLoading()) {
            return 1f;
        }

        // 每次缓存共 10 页
        return 1f * mLoadedPagesCount / 10;
    }

    /**
     * 10 位时间戳, 精确到秒
     */
    private long getCurrentTimeSecond() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * 开始缓存新的内容
     */
    private void startOffline() {
        final String contentKeyLoading = UUID.randomUUID().toString();
        CommonLog.d(TAG + " startOffline " + contentKeyLoading);

        mContentKeyLoading = contentKeyLoading;
        mLoadedPagesCount = 0;

        final Available finalAvailable = new Available() {
            @Override
            public boolean isAvailable() {
                return contentKeyLoading.equals(mContentKeyLoading);
            }
        };
        final long timeSecond = getCurrentTimeSecond();
        final List<Data.Joke> finalJokes = new ArrayList<>();

        mJokeApiService.getLastestJokes(timeSecond, 1)
                .mergeWith(mJokeApiService.getLastestJokes(timeSecond, 2))
                .mergeWith(mJokeApiService.getLastestJokes(timeSecond, 3))
                .mergeWith(mJokeApiService.getLastestJokes(timeSecond, 4))
                .mergeWith(mJokeApiService.getLastestJokes(timeSecond, 5))
                .mergeWith(mJokeApiService.getLastestJokes(timeSecond, 6))
                .mergeWith(mJokeApiService.getLastestJokes(timeSecond, 7))
                .mergeWith(mJokeApiService.getLastestJokes(timeSecond, 8))
                .mergeWith(mJokeApiService.getLastestJokes(timeSecond, 9))
                .mergeWith(mJokeApiService.getLastestJokes(timeSecond, 10))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<Data>() {
                    @Override
                    public void onCompleted() {
                        boolean available = finalAvailable.isAvailable();
                        CommonLog.d(TAG + " offline onCompleted " + contentKeyLoading + ", " + available);
                        if (!available) {
                            return;
                        }

                        if (!finalJokes.isEmpty()) {
                            // save joke
                            JokeOfflineInfo jokeOfflineInfo = saveOfflineJokes(contentKeyLoading, finalJokes);
                            if (jokeOfflineInfo != null) {
                                // 新的缓存成功保存，同步到内存
                                mJokeOfflineInfo = jokeOfflineInfo;
                                mOfflineJokesRef = new WeakReference<>(finalJokes);
                                return;
                            }
                        }

                        mContentKeyLoading = null;
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
                            throw new NotAvailableException();
                        }

                        mLoadedPagesCount++;

                        if (data != null
                                && data.error_code == 0
                                && data.result != null
                                && data.result.data != null
                                && !data.result.data.isEmpty()) {
                            // merge joke
                            for (Data.Joke joke : data.result.data) {
                                if (joke != null && !TextUtils.isEmpty(joke.content)) {
                                    finalJokes.add(joke);
                                }
                            }
                        }
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

    /**
     * 存储笑话内容到磁盘，如果存储成功，返回一个笑话内容信息的描述
     */
    @CheckResult
    private static JokeOfflineInfo saveOfflineJokes(String key, List<Data.Joke> jokes) {
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

    /**
     * 从磁盘中读取缓存的笑话内容
     */
    @CheckResult
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
        // 笑话内容比较长，可能不适宜在启动式直接加载到内存，使用 contentKey 来指向笑话内容在磁盘缓存中的 key
        public String contentKey;

        /**
         * 上一次缓存的笑话内容是否正确, 不关心当前是否正在缓存新的笑话内容
         */
        public boolean hasContent() {
            return !TextUtils.isEmpty(contentKey);
        }
    }

    public interface JokeApiService {

        /**
         * 十位的时间戳(精确到秒)，page 从 1 开始
         */
        // http://japi.juhe.cn/joke/content/list.from?key=您申请的KEY&page=2&pagesize=10&sort=asc&time=1418745237
        @GET("/joke/content/list.from?key=ac043f2d37f9b8cdadfbe16257d1c72e&pagesize=20&sort=desc")
        Observable<Data> getLastestJokes(@Query("time") long time, @Query("page") int page);

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
