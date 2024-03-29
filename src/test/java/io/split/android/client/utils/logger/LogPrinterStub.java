package io.split.android.client.utils.logger;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class LogPrinterStub implements LogPrinter {
    private final Set<Integer> calls = new HashSet<>();

    @Override
    public void v(String tag, String msg, Throwable tr) {
        calls.add(Log.VERBOSE);
    }

    @Override
    public void d(String tag, String msg, Throwable tr) {
        calls.add(Log.DEBUG);
    }

    @Override
    public void i(String tag, String msg, Throwable tr) {
        calls.add(Log.INFO);
    }

    @Override
    public void w(String tag, String msg, Throwable tr) {
        calls.add(Log.WARN);
    }

    @Override
    public void e(String tag, String msg, Throwable tr) {
        calls.add(Log.ERROR);
    }

    @Override
    public void wtf(String tag, String msg, Throwable tr) {
        calls.add(Log.ASSERT);
    }

    public boolean isCalled(Integer type) {
        return calls.contains(type);
    }
}
