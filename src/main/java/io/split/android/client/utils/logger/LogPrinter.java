package io.split.android.client.utils.logger;

import android.util.Log;

public interface LogPrinter {

    void v(String tag, String msg, Throwable tr);

    void d(String tag, String msg, Throwable tr);

    void i(String tag, String msg, Throwable tr);

    void w(String tag, String msg, Throwable tr);

    void e(String tag, String msg, Throwable tr);

    void wtf(String tag, String msg, Throwable tr);
}
