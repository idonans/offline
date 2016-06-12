package com.idonans.offline.news;

import android.net.Uri;
import android.support.annotation.CheckResult;
import android.text.TextUtils;

import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.idonans.acommon.data.OkHttpManager;
import com.idonans.acommon.data.StorageManager;
import com.idonans.acommon.lang.Available;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.Threads;
import com.idonans.acommon.util.AvailableUtil;
import com.idonans.offline.rx.SubscriptionHolder;

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
import rx.Subscription;
import rx.functions.Func1;
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
    private final SubscriptionHolder mSubscriptionHolderLoading = new SubscriptionHolder();

    private WeakReference<List<NewsDetailPreview>> mNewsDetailPreviewListRef;

    private NewsManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://www.tngou.net/")
                .client(OkHttpManager.getInstance().getOkHttpClient())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mNewsApiService = retrofit.create(NewsApiService.class);
        mNewsListOfflineInfo = restoreNewsListOfflineInfo();
    }

    private static NewsListOfflineInfo restoreNewsListOfflineInfo() {
        try {
            String json = StorageManager.getInstance().getCache(KEY_NEWS_LIST_OFFLINE_INFO);
            if (!TextUtils.isEmpty(json)) {
                Gson gson = new Gson();
                Type type = new TypeToken<NewsListOfflineInfo>() {
                }.getType();
                NewsListOfflineInfo newsListOfflineInfo = gson.fromJson(json, type);
                if (newsListOfflineInfo != null && newsListOfflineInfo.hasContent()) {
                    return newsListOfflineInfo;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        mSubscriptionHolderLoading.setSubscription(null);
    }

    /**
     * 上一次缓存成功的时间 ms
     */
    public long getOfflineNewsListTime() {
        if (mNewsListOfflineInfo != null && mNewsListOfflineInfo.hasContent()) {
            return mNewsListOfflineInfo.offlineTime;
        }
        return 0L;
    }

    public Observable<List<NewsDetailPreview>> getOfflineNewsList() {
        return Observable.create(new Observable.OnSubscribe<List<NewsDetailPreview>>() {
            @Override
            public void call(Subscriber<? super List<NewsDetailPreview>> subscriber) {
                List<NewsDetailPreview> offlineJokes = loadOfflineNewsListToMemory();
                subscriber.onNext(offlineJokes);
                subscriber.onCompleted();
            }
        });
    }

    public Observable<NewsDetail> getOfflineNewsDetail(final String offlineLocalDetailKey) {
        return Observable.create(new Observable.OnSubscribe<NewsDetail>() {
            @Override
            public void call(Subscriber<? super NewsDetail> subscriber) {
                NewsDetail newsDetail = loadOfflineNewsDetailToMemory(offlineLocalDetailKey);
                subscriber.onNext(newsDetail);
                subscriber.onCompleted();
            }
        });
    }

    private static NewsDetail loadOfflineNewsDetailToMemory(final String key) {
        try {
            String json = StorageManager.getInstance().getCache(key);
            if (!TextUtils.isEmpty(json)) {
                Gson gson = new Gson();
                Type type = new TypeToken<NewsDetail>() {
                }.getType();
                NewsDetail offlineNewsDetail = gson.fromJson(json, type);
                return offlineNewsDetail;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<NewsDetailPreview> loadOfflineNewsListToMemory() {
        if (mNewsDetailPreviewListRef != null) {
            List<NewsDetailPreview> offlineNewsList = mNewsDetailPreviewListRef.get();
            if (offlineNewsList != null) {
                return offlineNewsList;
            }
        }
        List<NewsDetailPreview> offlineNewsList = null;
        if (mNewsListOfflineInfo != null && mNewsListOfflineInfo.hasContent()) {
            offlineNewsList = restoreOfflineNewsList(mNewsListOfflineInfo.contentKey);
        }
        if (offlineNewsList == null) {
            offlineNewsList = new ArrayList<>();
        }
        mNewsDetailPreviewListRef = new WeakReference<>(offlineNewsList);
        return offlineNewsList;
    }

    @CheckResult
    private static List<NewsDetailPreview> restoreOfflineNewsList(String key) {
        try {
            String json = StorageManager.getInstance().getCache(key);
            if (!TextUtils.isEmpty(json)) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<NewsDetailPreview>>() {
                }.getType();
                List<NewsDetailPreview> offlineNewsList = gson.fromJson(json, type);
                return offlineNewsList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        final List<NewsDetailPreview> newsDetailPreviewList = new ArrayList<>();

        Subscription loadingSubscription = mNewsApiService.getLastestNewsList()
                .map(new Func1<NewsList, List<NewsDetailPreview>>() {
                    @Override
                    public List<NewsDetailPreview> call(NewsList newsList) {
                        boolean available = finalAvailable.isAvailable();
                        CommonLog.d(TAG + " offline NewsList flatMap call " + contentKeyLoading + ", " + available);
                        AvailableUtil.mustAvailable(finalAvailable);

                        List<NewsDetailPreview> newsDetailPreviewList = new ArrayList<NewsDetailPreview>();
                        if (newsList != null
                                && newsList.status
                                && newsList.tngou != null
                                && !newsList.tngou.isEmpty()) {
                            // load news detail

                            List<NewsList.Top> tops = newsList.tngou;
                            for (NewsList.Top top : tops) {
                                if (top != null
                                        && top.id > 0
                                        && top.time > 0
                                        && !TextUtils.isEmpty(top.title)
                                        && !TextUtils.isEmpty(top.description)
                                        && !TextUtils.isEmpty(top.img)) {
                                    NewsDetailPreview newsDetailPreview = new NewsDetailPreview();
                                    newsDetailPreview.id = top.id;
                                    newsDetailPreview.offlineLocalDetailKey = "_offline_local_news_detail_160503_" + top.id;
                                    newsDetailPreview.description = top.description;
                                    newsDetailPreview.time = top.time;
                                    newsDetailPreview.title = top.title;
                                    newsDetailPreview.imageCover = "http://tnfs.tngou.net/img" + top.img;
                                    newsDetailPreviewList.add(newsDetailPreview);
                                }
                            }
                        }

                        return newsDetailPreviewList;
                    }
                })
                .flatMap(new Func1<List<NewsDetailPreview>, Observable<NewsDetailPreview>>() {
                    @Override
                    public Observable<NewsDetailPreview> call(final List<NewsDetailPreview> newsDetailPreviews) {
                        if (newsDetailPreviews == null || newsDetailPreviews.isEmpty()) {
                            throw new IllegalArgumentException("news detail is empty");
                        }

                        AvailableUtil.mustAvailable(finalAvailable);
                        mNewsDetailCountForCache = newsDetailPreviews.size();

                        return Observable.from(newsDetailPreviews);
                    }
                })
                .flatMap(new Func1<NewsDetailPreview, Observable<NewsDetailPair>>() {
                    @Override
                    public Observable<NewsDetailPair> call(final NewsDetailPreview newsDetailPreview) {
                        return mNewsApiService.getNewsDetail(newsDetailPreview.id)
                                .map(new Func1<NewsDetail, NewsDetailPair>() {
                                    @Override
                                    public NewsDetailPair call(NewsDetail newsDetail) {
                                        boolean available = finalAvailable.isAvailable();
                                        CommonLog.d(TAG + " offline getNewsDetail map call " + contentKeyLoading + ", " + available);
                                        AvailableUtil.mustAvailable(finalAvailable);


                                        NewsDetailPair newsDetailPair = new NewsDetailPair();

                                        if (newsDetail == null
                                                || TextUtils.isEmpty(newsDetail.message)) {
                                            CommonLog.d(TAG + " news detail is null or message is empty, id:" + newsDetailPreview.id);
                                        } else {
                                            newsDetailPair.mNewsDetail = newsDetail;
                                            newsDetailPair.mNewsDetailPreview = newsDetailPreview;

                                            newsDetail.img = newsDetailPreview.imageCover;
                                            newsDetail.title = newsDetailPreview.title;
                                            newsDetail.time = newsDetailPreview.time;

                                            // 下载图片
                                            CommonLog.d(TAG + " try cache image to disk : " + newsDetail.img);
                                            ImageRequest imageRequest =
                                                    ImageRequestBuilder.newBuilderWithSource(Uri.parse(newsDetail.img))
                                                            .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH)
                                                            .build();
                                            DataSource<Void> dataSource = Fresco.getImagePipeline().prefetchToDiskCache(imageRequest, null, Priority.HIGH);
                                            // wait data source finish
                                            while (true) {
                                                if (dataSource.isFinished() || dataSource.hasFailed()) {
                                                    break;
                                                }

                                                AvailableUtil.mustAvailable(finalAvailable);

                                                Threads.sleepQuietly(2000);
                                            }
                                        }
                                        return newsDetailPair;
                                    }
                                });
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.immediate())
                .subscribe(new Observer<NewsDetailPair>() {
                    @Override
                    public void onCompleted() {
                        Threads.mustNotUi();
                        boolean available = finalAvailable.isAvailable();
                        CommonLog.d(TAG + " offline onCompleted " + contentKeyLoading + ", " + available);
                        if (!available) {
                            return;
                        }

                        if (!newsDetailPreviewList.isEmpty()) {
                            NewsListOfflineInfo newsListOfflineInfo = saveNewsDetailPreviewList(contentKeyLoading, newsDetailPreviewList);
                            if (newsListOfflineInfo != null) {
                                // 新的缓存成功保存，同步到内存
                                mNewsListOfflineInfo = newsListOfflineInfo;
                                mNewsDetailPreviewListRef = new WeakReference<>(newsDetailPreviewList);
                                return;
                            }
                        }

                        mContentKeyLoading = null;
                    }

                    @Override
                    public void onError(Throwable e) {
                        Threads.mustNotUi();
                        boolean available = finalAvailable.isAvailable();
                        CommonLog.d(TAG + " offline onError " + contentKeyLoading + ", " + available);
                        if (!available) {
                            return;
                        }
                        mContentKeyLoading = null;
                    }

                    @Override
                    public void onNext(NewsDetailPair newsDetailPair) {
                        Threads.mustNotUi();
                        boolean available = finalAvailable.isAvailable();
                        CommonLog.d(TAG + " offline onNext " + contentKeyLoading + ", " + available);
                        AvailableUtil.mustAvailable(finalAvailable);
                        mLoadedNewsDetailCount++;
                        if (saveDetail(newsDetailPair)) {
                            newsDetailPreviewList.add(newsDetailPair.mNewsDetailPreview);
                        }
                    }
                });
        mSubscriptionHolderLoading.setSubscription(loadingSubscription);
    }

    private NewsListOfflineInfo saveNewsDetailPreviewList(String key, List<NewsDetailPreview> newsDetailPreviewList) {
        if (TextUtils.isEmpty(key)) {
            key = UUID.randomUUID().toString();
        }
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<List<NewsDetailPreview>>() {
            }.getType();
            String json = gson.toJson(newsDetailPreviewList, type);
            if (!TextUtils.isEmpty(json)) {
                StorageManager.getInstance().setCache(key, json);

                NewsListOfflineInfo newsListOfflineInfo = new NewsListOfflineInfo();
                newsListOfflineInfo.contentKey = key;
                newsListOfflineInfo.offlineTime = System.currentTimeMillis();
                if (saveNewsListOfflineInfo(newsListOfflineInfo)) {
                    return newsListOfflineInfo;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean saveNewsListOfflineInfo(NewsListOfflineInfo newsListOfflineInfo) {
        if (newsListOfflineInfo != null && newsListOfflineInfo.hasContent()) {
            Gson gson = new Gson();
            Type type = new TypeToken<NewsListOfflineInfo>() {
            }.getType();
            String json = gson.toJson(newsListOfflineInfo, type);
            StorageManager.getInstance().setCache(KEY_NEWS_LIST_OFFLINE_INFO, json);
            return true;
        }
        return false;
    }

    private boolean saveDetail(NewsDetailPair newsDetailPair) {
        if (newsDetailPair == null
                || newsDetailPair.mNewsDetail == null
                || newsDetailPair.mNewsDetailPreview == null) {
            return false;
        }

        try {
            Gson gson = new Gson();
            Type type = new TypeToken<NewsDetail>() {
            }.getType();
            String json = gson.toJson(newsDetailPair.mNewsDetail, type);
            if (!TextUtils.isEmpty(json) && !TextUtils.isEmpty(newsDetailPair.mNewsDetailPreview.offlineLocalDetailKey)) {
                StorageManager.getInstance().setCache(newsDetailPair.mNewsDetailPreview.offlineLocalDetailKey, json);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
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

        /**
         * 获取详情
         */
        // http://www.tngou.net/api/top/show?id=00
        @GET("/api/top/show")
        Observable<NewsDetail> getNewsDetail(@Query("id") long id);

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

    public static class NewsDetailPreview {
        public long id;
        public String title;
        public String description;
        public String imageCover; // 封面图，完整路径
        public long time; // ms timestamp
        public String offlineLocalDetailKey;
    }

    public static class NewsDetail {
        public String title;
        public String message;
        public long time;
        public String img;
    }

    public static class NewsDetailPair {
        public NewsDetail mNewsDetail;
        public NewsDetailPreview mNewsDetailPreview;
    }

}
