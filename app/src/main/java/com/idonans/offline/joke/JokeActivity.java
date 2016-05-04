package com.idonans.offline.joke;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.offline.R;
import com.idonans.offline.rx.SubscriptionHolder;
import com.idonans.offline.util.ScrollHelper;

import java.util.List;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * 笑话
 * Created by idonans on 16-4-27.
 */
public class JokeActivity extends CommonActivity {

    private TextView mTitle;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;

    private SubscriptionHolder mSubscriptionHolderShown = new SubscriptionHolder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.joke_activity);

        Toolbar toolbar = ViewUtil.findViewByID(this, R.id.toolbar);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScrollHelper.scrollToHead(mRecyclerView);
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
        mTitle.setText("就是一个笑话");

        mRefreshLayout = ViewUtil.findViewByID(this, R.id.refresh_layout);

        mRecyclerView = ViewUtil.findViewByID(mRefreshLayout, R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showOfflineJokes();
            }
        });

        showOfflineJokes();
    }

    private void showOfflineJokes() {
        long offlineTime = JokeManager.getInstance().getOfflineJokesTime();
        if (offlineTime <= 0) {
            Toast.makeText(this, "暂无缓存", Toast.LENGTH_SHORT).show();
            return;
        }

        Subscription subscription = JokeManager.getInstance().getOfflineJokes()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<JokeManager.Data.Joke>>() {
                    @Override
                    public void onCompleted() {
                        mRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onNext(List<JokeManager.Data.Joke> jokes) {
                        if (jokes != null) {
                            mRecyclerView.setAdapter(new JokesAdapter(jokes));
                        }
                    }
                });
        mSubscriptionHolderShown.setSubscription(subscription);
    }

    private class JokesAdapter extends RecyclerView.Adapter {

        private List<JokeManager.Data.Joke> mOfflineJokes;

        public JokesAdapter(List<JokeManager.Data.Joke> offlineJokes) {
            mOfflineJokes = offlineJokes;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new JokeViewHolder(getLayoutInflater().inflate(R.layout.joke_activity_item, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((JokeViewHolder) holder).bind(mOfflineJokes.get(position), position);
        }

        @Override
        public int getItemCount() {
            return mOfflineJokes.size();
        }
    }

    private class JokeViewHolder extends RecyclerView.ViewHolder {

        private TextView mContent;

        public JokeViewHolder(View itemView) {
            super(itemView);
            mContent = ViewUtil.findViewByID(itemView, R.id.text_content);
        }

        public void bind(JokeManager.Data.Joke joke, int position) {
            if (joke == null) {
                mContent.setText(null);
            } else {
                mContent.setText(joke.content);
            }
            itemView.setSelected(position % 2 != 0);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSubscriptionHolderShown.setSubscription(null);
    }

}
