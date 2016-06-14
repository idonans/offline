package com.idonans.offline.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.idonans.acommon.util.DimenUtil;

/**
 * 适用于纵向线性布局的 divider, 可指定上下的空白区域, divider 的 size. 每个 ViewHolder需要有不透明背景
 * Created by idonans on 16-5-4.
 */
public class RecyclerViewSpaceItemDividerDecoration extends RecyclerView.ItemDecoration {

    private static final String TAG = "RecyclerViewSpaceItemDividerDecoration";
    private final int mTopArea;
    private final int mBottomArea;
    private final int mDividerSize;
    private final int mDividerColor;
    private final Paint mDividerPaint;

    public static RecyclerViewSpaceItemDividerDecoration defaultSize(int dividerColor) {
        int area = DimenUtil.dp2px(10);
        int dividerSize = 1;
        return new RecyclerViewSpaceItemDividerDecoration(area, area, dividerSize, dividerColor);
    }

    public RecyclerViewSpaceItemDividerDecoration(int topArea, int bottomArea, int dividerSize, int dividerColor) {
        if (dividerSize < 0) {
            throw new IllegalArgumentException("divider size must >= 0");
        }

        mTopArea = topArea;
        mBottomArea = bottomArea;
        mDividerSize = dividerSize;
        mDividerColor = dividerColor;
        mDividerPaint = new Paint();
        mDividerPaint.setColor(mDividerColor);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        // 第一个前面和最后一个后面有一定的空白区域, 每个之间有空白区域
        if (parent == null || parent.getAdapter() == null) {
            return;
        }

        int count = parent.getAdapter().getItemCount();
        if (count <= 0) {
            return;
        }

        int position = parent.getChildAdapterPosition(view);

        if (position < 0) {
            return;
        }

        if (position == 0) {
            outRect.top = mTopArea;
        }

        if (position == count - 1) {
            outRect.bottom = mBottomArea;
        } else {
            outRect.bottom = mDividerSize;
        }
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mDividerSize <= 0) {
            // ignore divider
            return;
        }

        int childCount = parent.getChildCount();
        if (childCount <= 1) {
            return;
        }

        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        for (int i = 0; i < childCount - 1; i++) {
            View childView = parent.getChildAt(i);
            // draw divider
            c.drawRect(left, childView.getBottom(), right, childView.getBottom() + mDividerSize, mDividerPaint);
        }
    }

}
