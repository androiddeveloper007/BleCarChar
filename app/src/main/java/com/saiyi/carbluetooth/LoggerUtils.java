package com.saiyi.carbluetooth;

import android.util.Log;


/**
 * Created by YGP on 2016/11/2.
 */
public class LoggerUtils {

    private static boolean isDebug = BuildConfig.DEBUG;

    private LoggerUtils() {

    }

    public static void v(String message) {
        if (isDebug) {
            Log.v("ZZP", message);
        }
    }

    public static void d(String message) {
        if (isDebug) {
            Log.d("ZZP", message);
        }
    }

    public static void i(String message) {
        if (true) {
            Log.i("ZZP", message);
        }
    }

    public static void w(String message) {
        if (true) {
            Log.w("ZZP", message);
        }
    }

    public static void e(Throwable throwable) {
        if (true) {
            Log.e("ZZP", "error", throwable);
        }
    }

    public static void e(String message) {
        if (isDebug) {
            Log.e("ZZP", message);
        }
    }


}
