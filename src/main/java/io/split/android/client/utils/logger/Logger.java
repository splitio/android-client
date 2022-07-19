package io.split.android.client.utils.logger;

/**
 * Created by sarrubia on 2/20/18.
 */

public class Logger {

    private static final String TAG = "SplitSDK";
    private int mLevel = SplitLogLevel.NONE;
    private static volatile Logger instance;
    private LogPrinter mLogPrinter = new LogPrinterImpl();

    private Logger() {
    }

    public static synchronized Logger instance() {
        if (instance == null) {
            synchronized (Logger.class) { // double checked locking principle to improve performance
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }

    public void setLevel(int logLevel) {
        mLevel = logLevel;
    }

    public void setPrinter(LogPrinter printer) {
        mLogPrinter = printer;
    }

    private void log(int priority, String msg, Throwable tr) {

        if (mLevel == SplitLogLevel.NONE || priority < SplitLogLevel.VERBOSE ||
                mLevel > priority) {
            return;
        }

        switch (priority) {
            case SplitLogLevel.VERBOSE:
                mLogPrinter.v(TAG, msg, tr);
                break;

            case SplitLogLevel.DEBUG:
                mLogPrinter.d(TAG, msg, tr);
                break;

            case SplitLogLevel.INFO:
                mLogPrinter.i(TAG, msg, tr);
                break;

            case SplitLogLevel.WARNING:
                mLogPrinter.w(TAG, msg, tr);
                break;

            case SplitLogLevel.ERROR:
                mLogPrinter.e(TAG, msg, tr);
                break;

            case SplitLogLevel.ASSERT:
                mLogPrinter.wtf(TAG, msg, tr);
                break;
        }
    }

    private static String formatMessage(String message, Object[] args) {
        if (args != null && args.length > 0) {
            message = String.format(message, args);
        }
        return message;
    }

    public static void v(String msg, Object... args) {
        v(formatMessage(msg, args));
    }

    public static void v(String msg) {
        instance().log(SplitLogLevel.VERBOSE, msg, null);
    }

    public static void v(String msg, Throwable tr) {
        instance().log(SplitLogLevel.VERBOSE, msg, tr);
    }

    public static void d(String msg, Object... args) {
        d(formatMessage(msg, args));
    }

    public static void d(String msg) {
        instance().log(SplitLogLevel.DEBUG, msg, null);
    }

    public static void d(String msg, Throwable tr) {
        instance().log(SplitLogLevel.DEBUG, msg, tr);
    }

    public static void i(String msg, Object... args) {
        i(formatMessage(msg, args));
    }

    public static void i(String msg) {
        instance().log(SplitLogLevel.INFO, msg, null);
    }

    public static void i(String msg, Throwable tr) {
        instance().log(SplitLogLevel.INFO, msg, tr);
    }

    public static void w(String msg, Object... args) {
        w(formatMessage(msg, args));
    }

    public static void w(Throwable tr, String msg, Object... args) {
        w(formatMessage(msg, args), tr);
    }

    public static void w(String msg) {
        instance().log(SplitLogLevel.WARNING, msg, null);
    }

    public static void w(String msg, Throwable tr) {
        instance().log(SplitLogLevel.WARNING, msg, tr);
    }

    public static void e(String msg, Object... args) {
        e(formatMessage(msg, args));
    }

    public static void e(Throwable tr, String msg, Object... args) {
        e(formatMessage(msg, args), tr);
    }

    public static void e(String msg) {
        instance().log(SplitLogLevel.ERROR, msg, null);
    }

    public static void e(String msg, Throwable tr) {
        instance().log(SplitLogLevel.ERROR, msg, tr);
    }

    public static void e(Throwable tr) {
        instance().log(SplitLogLevel.ERROR, "", tr);
    }

    public static void wtf(String msg, Object... args) {
        wtf(formatMessage(msg, args));
    }

    public static void wtf(String msg) {
        instance().log(SplitLogLevel.ASSERT, msg, null);
    }

    public static void wtf(String msg, Throwable tr) {
        instance().log(SplitLogLevel.ASSERT, msg, tr);
    }
}
