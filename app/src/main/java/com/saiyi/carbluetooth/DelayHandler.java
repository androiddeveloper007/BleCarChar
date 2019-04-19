package com.saiyi.carbluetooth;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class DelayHandler extends Handler {
    private DelayHandler(){
        super(Looper.getMainLooper());
    }


    private static class LazyHolder {
        private static final DelayHandler ME = new DelayHandler();
    }

    public static DelayHandler getInstance(){
        return LazyHolder.ME;
    }

    @Override
    public void handleMessage(Message msg) {
        Runnable run = (Runnable) msg.obj;
        run.run();
        super.handleMessage(msg);
    }

    public void sendDelayMessage(Runnable runnable) {
        sendDelayMessage(-1, runnable);
    }

    public void sendDelayMessage(long delayMillis, Runnable runnable) {
        Message msg = new Message();
        msg.obj = runnable;
        if (delayMillis > 0) {
            getInstance().sendMessageDelayed(msg, delayMillis);
        } else {
            getInstance().sendMessage(msg);
        }
    }
}
