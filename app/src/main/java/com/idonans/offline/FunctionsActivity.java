package com.idonans.offline;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.util.ViewUtil;

import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * 功能列表页
 * Created by idonans on 16-4-28.
 */
public class FunctionsActivity extends CommonActivity {

    private static final String TAG = "FunctionsActivity";

    private TextView mTitle;
    private RecyclerView mRecyclerView;
    private Subscription mSubscriptionShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.functions_activity);

        Toolbar toolbar = ViewUtil.findViewByID(this, R.id.toolbar);
        mTitle = ViewUtil.findViewByID(toolbar, R.id.title);
        mTitle.setText("离线阅读");

        mRecyclerView = ViewUtil.findViewByID(this, R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        showFunctions();
    }

    private void showFunctions() {
        Subscription subscription = FunctionsManager.getInstance().getFunctions()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<FunctionsManager.Function>>() {
                    @Override
                    public void call(List<FunctionsManager.Function> functions) {
                        if (functions != null) {
                            mRecyclerView.setAdapter(new FunctionsAdapter(functions));
                        }
                    }
                });
        setSubscriptionShown(subscription);
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

        public FunctionViewHolder(View itemView) {
            super(itemView);
            mFunctionTitle = ViewUtil.findViewByID(itemView, R.id.function_title);
        }

        public void bind(final FunctionsManager.Function function, int position) {
            mFunctionTitle.setText(function.getTitle());

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

}
