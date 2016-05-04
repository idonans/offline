package com.idonans.offline.util;

import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.RecyclerView;

/**
 * 辅助视图滚动
 * Created by idonans on 16-5-4.
 */
public class ScrollHelper {

    public static void scrollToHead(RecyclerView recyclerView) {
        if (recyclerView != null && recyclerView.getChildCount() > 0) {
            recyclerView.scrollToPosition(0);
        }
    }

    public static void scrollToHead(NestedScrollView scrollView) {
        if (scrollView != null && scrollView.getChildCount() > 0) {
            scrollView.scrollTo(0, 0);
        }
    }

}
