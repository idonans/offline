package com.idonans.offline.util;

import android.support.v7.widget.RecyclerView;

/**
 * 辅助操作 RecyclerView
 * Created by idonans on 16-5-4.
 */
public class RecyclerViewHelper {

    public static void scrollToHead(RecyclerView recyclerView) {
        if (recyclerView != null && recyclerView.getChildCount() > 0) {
            recyclerView.scrollToPosition(0);
        }
    }

}
