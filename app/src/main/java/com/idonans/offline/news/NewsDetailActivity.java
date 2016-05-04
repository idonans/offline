package com.idonans.offline.news;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.util.DimenUtil;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.offline.R;
import com.idonans.offline.rx.SubscriptionHolder;
import com.idonans.offline.util.ScrollHelper;

import java.util.Locale;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * 新闻详情
 * Created by idonans on 16-5-3.
 */
public class NewsDetailActivity extends CommonActivity {

    private static final String EXTRA_LOCAL_NEWS_DETAIL_KEY = "extra.LOCAL_NEWS_DETAIL_KEY";

    private TextView mTitle;

    private NestedScrollView mScrollView;
    private SimpleDraweeView mNewsCover;
    private TextView mNewsTitle;
    private TextView mNewsTime;
    private TextView mNewsContent;

    private int mCoverWidth;
    private int mCoverHeight;

    private SubscriptionHolder mSubscriptionHolderShown = new SubscriptionHolder();

    private String mLocalNewsDetailKey;

    public static Intent newIntent(Context context, String localNewsDetailKey) {
        Intent intent = new Intent(context, NewsDetailActivity.class);
        intent.putExtra(EXTRA_LOCAL_NEWS_DETAIL_KEY, localNewsDetailKey);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news_detail_activity);

        mCoverWidth = DimenUtil.dp2px(360);
        mCoverHeight = DimenUtil.dp2px(360);

        final Toolbar toolbar = ViewUtil.findViewByID(this, R.id.toolbar);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScrollHelper.scrollToHead(mScrollView);
            }
        });
        View backPanel = ViewUtil.findViewByID(toolbar, R.id.back_panel);
        backPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mTitle = ViewUtil.findViewByID(backPanel, R.id.view_title);
        mTitle.setText("新闻详情");

        mScrollView = ViewUtil.findViewByID(this, R.id.scroll_view);
        mNewsCover = ViewUtil.findViewByID(mScrollView, R.id.news_cover);
        mNewsTitle = ViewUtil.findViewByID(mScrollView, R.id.news_title);
        mNewsTime = ViewUtil.findViewByID(mScrollView, R.id.news_time);
        mNewsContent = ViewUtil.findViewByID(mScrollView, R.id.news_content);

        mLocalNewsDetailKey = getIntent().getStringExtra(EXTRA_LOCAL_NEWS_DETAIL_KEY);

        showNewsDetail();
    }

    private void showNewsDetail() {
        Subscription subscription = NewsManager.getInstance().getOfflineNewsDetail(mLocalNewsDetailKey)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<NewsManager.NewsDetail>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(NewsDetailActivity.this, "载入失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(NewsManager.NewsDetail newsDetail) {
                        if (newsDetail == null) {
                            throw new Resources.NotFoundException("new detail not found #" + mLocalNewsDetailKey);
                        }

                        ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(newsDetail.img))
                                .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.DISK_CACHE)
                                .setResizeOptions(new ResizeOptions(mCoverWidth, mCoverHeight))
                                .setAutoRotateEnabled(true)
                                .build();
                        DraweeController controller = Fresco.newDraweeControllerBuilder()
                                .setImageRequest(imageRequest)
                                .setAutoPlayAnimations(true)
                                .build();
                        mNewsCover.setController(controller);

                        mNewsTitle.setText(newsDetail.title);
                        CharSequence newsContent = newsDetail.message;
                        try {
                            newsContent = Html.fromHtml(newsDetail.message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mNewsTime.setText(String.format(Locale.SIMPLIFIED_CHINESE, "%tF %tR", newsDetail.time, newsDetail.time));
                        mNewsContent.setText(newsContent);
                    }
                });
        mSubscriptionHolderShown.setSubscription(subscription);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSubscriptionHolderShown.setSubscription(null);
    }

}
