package com.idonans.offline;

import android.content.Context;
import android.content.Intent;

import com.idonans.offline.joke.JokeActivity;
import com.idonans.offline.joke.JokeManager;
import com.idonans.offline.news.NewsListActivity;
import com.idonans.offline.news.NewsManager;
import com.idonans.offline.share.sinaweibo.ShareSinaWeiboActivity;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by idonans on 16-4-28.
 */
public class FunctionsManager {

    private static class InstanceHolder {

        private static final FunctionsManager sInstance = new FunctionsManager();

    }

    public static FunctionsManager getInstance() {
        return InstanceHolder.sInstance;
    }

    private List<Function> mFunctions;

    private FunctionsManager() {
    }

    public Observable<List<Function>> getFunctions() {
        return Observable.create(new Observable.OnSubscribe<List<Function>>() {
            @Override
            public void call(Subscriber<? super List<Function>> subscriber) {
                initFunctions();
                subscriber.onNext(mFunctions);
                subscriber.onCompleted();
            }
        });
    }

    private void initFunctions() {
        if (mFunctions != null) {
            return;
        }

        List<Function> functions = new ArrayList<>();
        functions.add(new JokeFunction());
        functions.add(new NewsListFunctions());
        functions.add(new ShareSinaWeiboFunction());

        mFunctions = functions;
    }

    public void startOffline() {
        if (mFunctions != null) {
            for (Function function : mFunctions) {
                function.startOffline();
            }
        }
    }

    public boolean isLoading() {
        if (mFunctions != null) {
            for (Function function : mFunctions) {
                if (function.isLoading()) {
                    return true;
                }
            }
        }
        return false;
    }

    public interface Function {
        boolean isLoading();

        /**
         * 数据加载进度 [0f, 1f]
         */
        float getLoadingProgress();

        long getOfflineTime();

        String getTitle();

        void startOffline();

        void cancel();

        void startActivity(Context context);
    }

    /**
     * 笑话
     */
    private class JokeFunction implements Function {

        private final JokeManager mJokeManager = JokeManager.getInstance();

        @Override
        public boolean isLoading() {
            return mJokeManager.isLoading();
        }

        @Override
        public float getLoadingProgress() {
            return mJokeManager.getLoadingProgress();
        }

        @Override
        public long getOfflineTime() {
            return mJokeManager.getOfflineJokesTime();
        }

        @Override
        public String getTitle() {
            return "就是一个笑话";
        }

        @Override
        public void startOffline() {
            mJokeManager.offline(false);
        }

        @Override
        public void cancel() {
            mJokeManager.cancel();
        }

        @Override
        public void startActivity(Context context) {
            Intent intent = new Intent(context, JokeActivity.class);
            context.startActivity(intent);
        }
    }

    /**
     * 新闻热点
     */
    private class NewsListFunctions implements Function {

        private final NewsManager mNewsManager = NewsManager.getInstance();

        @Override
        public boolean isLoading() {
            return mNewsManager.isLoading();
        }

        @Override
        public float getLoadingProgress() {
            return mNewsManager.getLoadingProgress();
        }

        @Override
        public long getOfflineTime() {
            return mNewsManager.getOfflineNewsListTime();
        }

        @Override
        public String getTitle() {
            return "新闻热点";
        }

        @Override
        public void startOffline() {
            mNewsManager.offline(false);
        }

        @Override
        public void cancel() {
            mNewsManager.cancel();
        }

        @Override
        public void startActivity(Context context) {
            Intent intent = new Intent(context, NewsListActivity.class);
            context.startActivity(intent);
        }
    }

    /**
     * Sina 微博登陆授权分享
     */
    private class ShareSinaWeiboFunction implements Function {

        @Override
        public boolean isLoading() {
            return false;
        }

        /**
         * 数据加载进度 [0f, 1f]
         */
        @Override
        public float getLoadingProgress() {
            return 0;
        }

        @Override
        public long getOfflineTime() {
            return 0;
        }

        @Override
        public String getTitle() {
            return "新浪微博授权分享";
        }

        @Override
        public void startOffline() {
        }

        @Override
        public void cancel() {
        }

        @Override
        public void startActivity(Context context) {
            Intent intent = new Intent(context, ShareSinaWeiboActivity.class);
            context.startActivity(intent);
        }
    }

}
