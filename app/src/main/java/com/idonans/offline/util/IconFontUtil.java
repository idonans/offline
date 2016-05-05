package com.idonans.offline.util;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.widget.TextView;

import com.idonans.acommon.AppContext;

/**
 * 设置 icon font 字体
 * Created by idonans on 16-5-5.
 */
public class IconFontUtil {

    public static void setIconFont(TextView textView, String fontPath) {
        if (textView == null) {
            return;
        }
        if (TextUtils.isEmpty(fontPath)) {
            return;
        }

        try {
            Typeface iconFont = Typeface.createFromAsset(AppContext.getContext().getAssets(), fontPath);
            textView.setTypeface(iconFont);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
