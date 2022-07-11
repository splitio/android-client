package io.split.android.client.utils.logger;

import android.util.Log;

public class LogPrinterImpl implements LogPrinter {
    @Override
    public void v(String tag, String msg, Throwable tr) {
        Log.v(tag, msg);
    }

    @Override
    public void d(String tag, String msg, Throwable tr) {
        Log.d(tag, msg, tr);
    }

    @Override
    public void i(String tag, String msg, Throwable tr) {
        Log.i(tag, msg, tr);
    }

    @Override
    public void w(String tag, String msg, Throwable tr) {
        Log.w(tag, msg, tr);
    }

    @Override
    public void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
    }

    @Override
    public void wtf(String tag, String msg, Throwable tr) {
        Log.wtf(tag, msg, tr);
    }
}
