package com.idonans.offline.joke;

import android.os.Bundle;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 笑话
 * Created by idonans on 16-4-27.
 */
public class JokeActivity extends CommonActivity {

    private TextView mTitle;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.joke_activity);

        Toolbar toolbar = ViewUtil.findViewByID(this, R.id.toolbar);
        View backPanel = ViewUtil.findViewByID(toolbar, R.id.back_panel);
        backPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mTitle = ViewUtil.findViewByID(backPanel, R.id.title);
        mTitle.setText("笑话");

        mRecyclerView = ViewUtil.findViewByID(this, R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        showOfflineJokes();
    }

    private void showOfflineJokes() {
        long offlineTime = JokeManager.getInstance().getOfflineJokesTime();
        if (offlineTime <= 0) {
            mTitle.setText("笑话");
            Toast.makeText(this, "暂无缓存", Toast.LENGTH_SHORT).show();
            return;
        }

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(offlineTime));
        mTitle.setText(time);

        List<JokeManager.Data.Joke> offlineJokes = JokeManager.getInstance().getOfflineJokes();
        if (offlineJokes == null || offlineJokes.isEmpty()) {
            Toast.makeText(this, "暂无缓存", Toast.LENGTH_SHORT).show();
            return;
        }

        mRecyclerView.setAdapter(new JokesAdapter(offlineJokes));
    }

    private class JokesAdapter extends RecyclerView.Adapter {

        private List<JokeManager.Data.Joke> mOfflineJokes;

        public JokesAdapter(List<JokeManager.Data.Joke> offlineJokes) {
            mOfflineJokes = offlineJokes;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(JokeActivity.this);
            return new JokeViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((JokeViewHolder) holder).bind(mOfflineJokes.get(position));
        }

        @Override
        public int getItemCount() {
            return mOfflineJokes.size();
        }
    }

    private class JokeViewHolder extends RecyclerView.ViewHolder {

        private TextView mContent;

        public JokeViewHolder(TextView itemView) {
            super(itemView);
            mContent = itemView;
        }

        public void bind(JokeManager.Data.Joke joke) {
            if (joke == null) {
                mContent.setText(null);
            } else {
                mContent.setText(joke.content);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        JokeManager.getInstance().offline(false);
    }

}
