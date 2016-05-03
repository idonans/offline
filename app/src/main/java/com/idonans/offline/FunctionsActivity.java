package com.idonans.offline;

import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.TaskQueue;
import com.idonans.acommon.lang.Threads;
import com.idonans.acommon.lang.WeakAvailable;
import com.idonans.acommon.util.DimenUtil;
import com.idonans.acommon.util.NetUtil;
import com.idonans.acommon.util.ViewUtil;

import java.util.List;
import java.util.Locale;

import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * 功能列表页
 * Created by idonans on 16-4-28.
 */
public class FunctionsActivity extends CommonActivity {

    private static final String TAG = "FunctionsActivity";

    private TextView mTitle;
    private TextView mOfflineProgress;
    private ImageView mMore;

    private final TaskQueue mOfflineProgressSyncQueue = new TaskQueue(1);

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;
    private Subscription mSubscriptionShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.functions_activity);

        final Toolbar toolbar = ViewUtil.findViewByID(this, R.id.toolbar);
        mTitle = ViewUtil.findViewByID(toolbar, R.id.view_title);
        mTitle.setText("离线阅读");

        mOfflineProgress = ViewUtil.findViewByID(toolbar, R.id.offline_progress);

        mMore = ViewUtil.findViewByID(toolbar, R.id.more);
        mMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreDialog();
            }
        });

        mRefreshLayout = ViewUtil.findViewByID(this, R.id.refresh_layout);

        mRecyclerView = ViewUtil.findViewByID(mRefreshLayout, R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.addItemDecoration(new FunctionsItemDivider());

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showFunctions();
            }
        });

        showFunctions();
    }

    private void showFunctions() {
        Subscription subscription = FunctionsManager.getInstance().getFunctions()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<FunctionsManager.Function>>() {
                    @Override
                    public void onCompleted() {
                        mRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onNext(List<FunctionsManager.Function> functions) {
                        if (functions != null) {
                            mRecyclerView.setAdapter(new FunctionsAdapter(functions));
                        }
                    }
                });
        setSubscriptionShown(subscription);
    }


    @Override
    protected void onResume() {
        super.onResume();
        syncOfflineProgress();
    }

    private OfflineProgressTask mOfflineProgressTask;

    private void syncOfflineProgress() {
        mOfflineProgressTask = new OfflineProgressTask(this) {
            @Override
            public boolean isAvailable() {
                return mOfflineProgressTask == this && super.isAvailable();
            }
        };
        mOfflineProgressSyncQueue.enqueue(mOfflineProgressTask);
    }

    private static class OfflineProgressTask extends WeakAvailable implements Runnable {

        private boolean mFirst = true;

        public OfflineProgressTask(FunctionsActivity functionsActivity) {
            super(functionsActivity);
        }

        @Override
        public void run() {
            CommonLog.d(TAG + " OfflineProgressTask run @" + hashCode());
            Threads.mustNotUi();

            // 第一次时不做延迟
            if (!mFirst) {
                Threads.sleepQuietly(2000);
            }
            mFirst = false;

            final boolean loading = FunctionsManager.getInstance().isLoading();

            FunctionsActivity functionsActivity = (FunctionsActivity) getObject();
            if (!isAvailable()) {
                return;
            }
            if (functionsActivity.isPaused()) {
                return;
            }

            functionsActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    FunctionsActivity functionsActivity = (FunctionsActivity) getObject();
                    if (!isAvailable()) {
                        return;
                    }
                    if (functionsActivity.isPaused()) {
                        return;
                    }

                    if (loading) {
                        functionsActivity.mOfflineProgress.setText("(下载中)");
                    } else {
                        functionsActivity.mOfflineProgress.setText(null);
                    }

                    // 开始下一次循环
                    functionsActivity.mOfflineProgressSyncQueue.enqueue(OfflineProgressTask.this);
                }
            });
        }

    }

    private class FunctionsItemDivider extends RecyclerView.ItemDecoration {

        private final Paint mPaintDivider;
        private final int mEmptyArea;

        private FunctionsItemDivider() {
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

    private class FunctionsAdapter extends RecyclerView.Adapter {

        private final List<FunctionsManager.Function> mFunctions;

        private FunctionsAdapter(List<FunctionsManager.Function> functions) {
            mFunctions = functions;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FunctionViewHolder(getLayoutInflater().inflate(R.layout.functions_activity_item, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((FunctionViewHolder) holder).bind(mFunctions.get(position), position);
        }

        @Override
        public int getItemCount() {
            return mFunctions.size();
        }
    }

    private class FunctionViewHolder extends RecyclerView.ViewHolder {

        private TextView mFunctionTitle;
        private TextView mFunctionOfflineTime;

        public FunctionViewHolder(View itemView) {
            super(itemView);
            mFunctionTitle = ViewUtil.findViewByID(itemView, R.id.function_title);
            mFunctionOfflineTime = ViewUtil.findViewByID(itemView, R.id.function_offline_time);
        }

        public void bind(final FunctionsManager.Function function, int position) {
            mFunctionTitle.setText(function.getTitle());

            StringBuilder offlineTimeBuffer = new StringBuilder();
            long offlineTime = function.getOfflineTime();
            boolean loading = function.isLoading();
            if (offlineTime > 0) {
                offlineTimeBuffer.append(String.format(Locale.SIMPLIFIED_CHINESE, "上次更新：%tF %tR", offlineTime, offlineTime));
                if (loading) {
                    offlineTimeBuffer.append(" [正在更新]");
                }
            } else {
                if (loading) {
                    offlineTimeBuffer.append("[正在更新]");
                } else {
                    offlineTimeBuffer.append("无更新记录");
                }
            }
            mFunctionOfflineTime.setText(offlineTimeBuffer);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    function.startActivity(FunctionsActivity.this);
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

    private void showMoreDialog() {
        if (isPaused()) {
            return;
        }

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(R.layout.functions_activity_more_dialog)
                .create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                final AlertDialog alertDialog = (AlertDialog) dialog;
                final View networkWarnTip = alertDialog.findViewById(R.id.network_warn_tip);
                final View networkWarnConfirm = alertDialog.findViewById(R.id.network_warn_confirm);
                final View startOffline = alertDialog.findViewById(R.id.start_offline);

                if (networkWarnTip == null
                        || networkWarnConfirm == null
                        || startOffline == null) {
                    CommonLog.d(TAG + " more dialog init fail");
                    return;
                }

                networkWarnConfirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.setSelected(!v.isSelected());
                        startOffline.setEnabled(v.isSelected());
                    }
                });

                startOffline.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FunctionsManager.getInstance().startOffline();
                        alertDialog.dismiss();
                    }
                });

                if (NetUtil.isActiveNetworkMetered()) {
                    networkWarnTip.setVisibility(View.VISIBLE);
                    networkWarnConfirm.setVisibility(View.VISIBLE);
                    networkWarnConfirm.setSelected(false);
                    startOffline.setEnabled(false);
                } else {
                    networkWarnTip.setVisibility(View.GONE);
                    networkWarnConfirm.setVisibility(View.GONE);
                    networkWarnConfirm.setSelected(false);
                    startOffline.setEnabled(true);
                }
            }
        });
        alertDialog.show();
    }

}
