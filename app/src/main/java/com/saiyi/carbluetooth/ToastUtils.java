package com.saiyi.carbluetooth;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils {

    private static Toast toast;

    private ToastUtils() {
    }

    public static void makeText(Context c,CharSequence text) {
        if (text.toString().equals("") || text.toString().equals("1804")) return;//屏蔽状态码1804
        if (toast == null) {
            toast = Toast.makeText(c, text, Toast.LENGTH_SHORT);
        }
        toast.setText(text);
        toast.show();
    }

    public static void makeText(Context c, int resId) {
        if (toast == null) {
            toast = Toast.makeText(c, resId, Toast.LENGTH_SHORT);
        }
        toast.setText(resId);
        toast.show();
    }
}
