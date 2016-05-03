package com.idonans.offline.news;

import android.text.TextUtils;

import com.idonans.offline.data.HttpManager;

import java.util.List;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import rx.Observable;

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

    private NewsManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://www.tngou.net/")
                .client(HttpManager.getInstance().getOkHttpClient())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mNewsApiService = retrofit.create(NewsApiService.class);
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
