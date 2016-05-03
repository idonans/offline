package com.idonans.offline.news;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

import java.util.List;
import java.util.Locale;

import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * 新闻热点列表
 * Created by idonans on 16-5-3.
 */
public class NewsListActivity extends CommonActivity {

    private TextView mTitle;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;
    private Subscription mSubscriptionShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news_list_activity);

        final Toolbar toolbar = ViewUtil.findViewByID(this, R.id.toolbar);
        mTitle = ViewUtil.findViewByID(toolbar, R.id.view_title);
        mTitle.setText("新闻热点");

        mRefreshLayout = ViewUtil.findViewByID(this, R.id.refresh_layout);

        mRecyclerView = ViewUtil.findViewByID(mRefreshLayout, R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.addItemDecoration(new NewsItemDivider());

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showNewsList();
            }
        });

        showNewsList();
    }

    private void showNewsList() {
        Subscription subscription = NewsManager.getInstance().getOfflineNewsList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<NewsManager.NewsDetailPreview>>() {
                    @Override
                    public void onCompleted() {
                        mRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onNext(List<NewsManager.NewsDetailPreview> newsDetailPreviewList) {
                        if (newsDetailPreviewList != null) {
                            mRecyclerView.setAdapter(new NewsListAdapter(newsDetailPreviewList));
                        }
                    }
                });
        setSubscriptionShown(subscription);
    }

    private class NewsItemDivider extends RecyclerView.ItemDecoration {

        private final Paint mPaintDivider;
        private final int mEmptyArea;

        private NewsItemDivider() {
            mPaintDivider = new Paint();
            mPaintDivider.setColor(Color.DKGRAY);
            mEmptyArea = DimenUtil.dp2px(20);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            // 第一个前面和最后一个后面有一定的空白区域, 每个之间有空白区域
            int count = parent.getAdapter().getItemCount();

            int position = parent.getChildAdapterPosition(view);
            if (position == 0) {
                outRect.top = mEmptyArea;
            }

            if (position == count - 1) {
                outRect.bottom = mEmptyArea;
            } else {
                outRect.bottom = 1;
            }
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) parent.getLayoutManager();
            int firstPosition = layoutManager.findFirstVisibleItemPosition();
            int lastPosition = layoutManager.findLastVisibleItemPosition();

            if (firstPosition < 0 || lastPosition < 0 || firstPosition == lastPosition) {
                return;
            }

            View firstView = layoutManager.findViewByPosition(firstPosition);
            View lastView = layoutManager.findViewByPosition(lastPosition);
            if (firstView == null || lastView == null) {
                return;
            }

            c.drawRect(0, firstView.getTop(), parent.getWidth(), lastView.getBottom(), mPaintDivider);
        }

    }

    private class NewsListAdapter extends RecyclerView.Adapter {

        private final List<NewsManager.NewsDetailPreview> mNewsList;

        private NewsListAdapter(List<NewsManager.NewsDetailPreview> newsList) {
            mNewsList = newsList;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new NewsItemViewHolder(getLayoutInflater().inflate(R.layout.news_list_activity_item, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((NewsItemViewHolder) holder).bind(mNewsList.get(position), position);
        }

        @Override
        public int getItemCount() {
            return mNewsList.size();
        }
    }

    private class NewsItemViewHolder extends RecyclerView.ViewHolder {

        private SimpleDraweeView mNewsCover;
        private TextView mNewsTitle;
        private TextView mNewsDesc;
        private TextView mNewsTime;
        private final int mCoverWidth;
        private final int mCoverHeight;

        public NewsItemViewHolder(View itemView) {
            super(itemView);
            mCoverWidth = DimenUtil.dp2px(120);
            mCoverHeight = DimenUtil.dp2px(120);
            mNewsCover = ViewUtil.findViewByID(itemView, R.id.news_cover);
            mNewsTitle = ViewUtil.findViewByID(itemView, R.id.news_title);
            mNewsDesc = ViewUtil.findViewByID(itemView, R.id.news_desc);
            mNewsTime = ViewUtil.findViewByID(itemView, R.id.news_time);
        }

        public void bind(final NewsManager.NewsDetailPreview newsItem, int position) {
            ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(newsItem.imageCover))
                    .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.DISK_CACHE)
                    .setResizeOptions(new ResizeOptions(mCoverWidth, mCoverHeight))
                    .build();
            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(imageRequest)
                    .build();
            mNewsCover.setController(controller);

            mNewsTitle.setText(newsItem.title);
            mNewsDesc.setText(newsItem.description);
            mNewsTime.setText(String.format(Locale.SIMPLIFIED_CHINESE, "%tF %tR", newsItem.time, newsItem.time));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO
                }
            });
        }

    }

    public void setSubscriptionShown(Subscription subscriptionShown) {
        if (mSubscriptionShown != null) {
            mSubscriptionShown.unsubscribe();
        }
        mSubscriptionShown = subscriptionShown;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setSubscriptionShown(null);
    }

}
