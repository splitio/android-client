package io.split.android.client.utils.logger;

import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LogPrinterStub implements LogPrinter {
    private final Set<Integer> calls = new HashSet<>();
    private final Map<Integer, ConcurrentLinkedDeque<String>> logs = new ConcurrentHashMap<>();

    public LogPrinterStub() {
        // Initialize for all Android log levels: VERBOSE(2) .. ASSERT(7)
        for (int level = Log.VERBOSE; level <= Log.ASSERT; level++) {
            logs.put(level, new ConcurrentLinkedDeque<>());
        }
    }

    @Override
    public void v(String tag, String msg, Throwable tr) {
        logs.get(Log.VERBOSE).add(msg);
        calls.add(Log.VERBOSE);
    }

    @Override
    public void d(String tag, String msg, Throwable tr) {
        logs.get(Log.DEBUG).add(msg);
        calls.add(Log.DEBUG);
    }

    @Override
    public void i(String tag, String msg, Throwable tr) {
        logs.get(Log.INFO).add(msg);
        calls.add(Log.INFO);
    }

    @Override
    public void w(String tag, String msg, Throwable tr) {
        logs.get(Log.WARN).add(msg);
        calls.add(Log.WARN);
    }

    @Override
    public void e(String tag, String msg, Throwable tr) {
        logs.get(Log.ERROR).add(msg);
        calls.add(Log.ERROR);
    }

    @Override
    public void wtf(String tag, String msg, Throwable tr) {
        logs.get(Log.ASSERT).add(msg);
        calls.add(Log.ASSERT);
    }

    public boolean isCalled(Integer type) {
        return calls.contains(type);
    }

    public Map<Integer, ConcurrentLinkedDeque<String>> getLoggedMessages() {
        return new HashMap<>(logs);
    }
}
