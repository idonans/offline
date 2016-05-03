package com.idonans.offline.news;

import android.text.TextUtils;

import com.idonans.acommon.lang.Available;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.util.AvailableUtil;
import com.idonans.offline.data.HttpManager;

import java.util.List;
import java.util.UUID;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

/**
 * 新闻热点
 * Created by idonans on 16-5-3.
 */
public class NewsManager {

    private static class InstanceHolder {

        private static final NewsManager sInstance = new NewsManager();

    }

    public static NewsManager getInstance() {
        return InstanceHolder.sInstance;
    }

    private static final String TAG = "NewsManager";
    private final NewsApiService mNewsApiService;

    private static final String KEY_NEWS_LIST_OFFLINE_INFO = "offline_news_list_offline_info";
    private NewsListOfflineInfo mNewsListOfflineInfo;

    // 如果当前正在缓存新的新闻热词，用来临时记录一个缓存 key, 当新的新闻热词缓存成功时，此 key 会被写入 NewsListOfflineInfo
    private String mContentKeyLoading;
    // 如果当前正在缓存新的新闻热词，此值用来记录已经缓存的新闻热词详情条数
    private int mLoadedNewsDetailCount;
    // 如果当前正在缓存新的新闻热词，此值用来记录本次需要缓存的新闻热词详情的总条数
    private int mNewsDetailCountForCache;

    private NewsManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://www.tngou.net/")
                .client(HttpManager.getInstance().getOkHttpClient())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mNewsApiService = retrofit.create(NewsApiService.class);
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
        if (mNewsListOfflineInfo != null && mNewsListOfflineInfo.hasContent()) {
            return mNewsListOfflineInfo.offlineTime;
        }
        return 0L;
    }

    /**
     * 是否正在缓存新的内容
     */
    public boolean isLoading() {
        if (TextUtils.isEmpty(mContentKeyLoading)) {
            return false;
        }
        if (mNewsListOfflineInfo != null && mContentKeyLoading.equals(mNewsListOfflineInfo.contentKey)) {
            return false;
        }
        return true;
    }

    public float getLoadingProgress() {
        if (!isLoading()) {
            return 1f;
        }

        if (mNewsDetailCountForCache <= 0) {
            return 0f;
        }
        return 1f * mLoadedNewsDetailCount / mNewsDetailCountForCache;
    }

    /**
     * 开始缓存新的新闻热词列表和详情
     */
    private void startOffline() {
        final String contentKeyLoading = UUID.randomUUID().toString();
        CommonLog.d(TAG + " startOffline " + contentKeyLoading);

        mContentKeyLoading = contentKeyLoading;
        mLoadedNewsDetailCount = 0;
        mNewsDetailCountForCache = 0;

        final Available finalAvailable = new Available() {
            @Override
            public boolean isAvailable() {
                return contentKeyLoading.equals(mContentKeyLoading);
            }
        };

        mNewsApiService.getLastestNewsList()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<NewsList>() {
                    @Override
                    public void onCompleted() {
                        boolean available = finalAvailable.isAvailable();
                        CommonLog.d(TAG + " offline onCompleted " + contentKeyLoading + ", " + available);
                        if (!available) {
                            return;
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
                    public void onNext(NewsList newsList) {
                        boolean available = finalAvailable.isAvailable();
                        CommonLog.d(TAG + " offline onNext " + contentKeyLoading + ", " + available);
                        AvailableUtil.mustAvailable(finalAvailable);

                        if (newsList != null
                                && newsList.status
                                && newsList.tngou != null
                                && !newsList.tngou.isEmpty()) {
                            // load news detail
                        }
                    }
                });
    }

    private static class NewsListOfflineInfo {
        public long offlineTime;
        // 热词列表内容比较长，不适宜在启动式直接加载到内存，使用 contentKey 来指向热词列表在磁盘缓存中的 key
        public String contentKey;

        /**
         * 校验上一次缓存的热词列表是否正确, 不关心当前是否正在缓存新的热词列表
         */
        public boolean hasContent() {
            return !TextUtils.isEmpty(contentKey);
        }
    }

    public interface NewsApiService {

        /**
         * 最新 100 条新闻热词
         */
        // http://www.tngou.net/api/top/list?rows=100
        @GET("/api/top/list?rows=100")
        Observable<NewsList> getLastestNewsList();

    }

    public static class NewsList {

        public boolean status;
        public List<Top> tngou;

        public static class Top {
            public long id;
            public String title;
            public String description;
            public String img;
            public long time; // ms timestamp
        }
    }

}
