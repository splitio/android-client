package io.split.android.client.utils.logger;

/**
 * Created by sarrubia on 2/20/18.
 */

public class Logger {

    private static final String TAG = "SplitSDK";
    private int mLevel = Level.NONE;
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

        if (mLevel == Level.NONE || priority < Level.VERBOSE ||
                mLevel > priority) {
            return;
        }

        switch (priority) {
            case Level.VERBOSE:
                mLogPrinter.v(TAG, msg, tr);
                break;

            case Level.DEBUG:
                mLogPrinter.d(TAG, msg, tr);
                break;

            case Level.INFO:
                mLogPrinter.i(TAG, msg, tr);
                break;

            case Level.WARNING:
                mLogPrinter.w(TAG, msg, tr);
                break;

            case Level.ERROR:
                mLogPrinter.e(TAG, msg, tr);
                break;

            case Level.ASSERT:
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
        instance().log(Level.VERBOSE, msg, null);
    }

    public static void v(String msg, Throwable tr) {
        instance().log(Level.VERBOSE, msg, tr);
    }

    public static void d(String msg, Object... args) {
        d(formatMessage(msg, args));
    }

    public static void d(String msg) {
        instance().log(Level.DEBUG, msg, null);
    }

    public static void d(String msg, Throwable tr) {
        instance().log(Level.DEBUG, msg, tr);
    }

    public static void i(String msg, Object... args) {
        i(formatMessage(msg, args));
    }

    public static void i(String msg) {
        instance().log(Level.INFO, msg, null);
    }

    public static void i(String msg, Throwable tr) {
        instance().log(Level.INFO, msg, tr);
    }

    public static void w(String msg, Object... args) {
        w(formatMessage(msg, args));
    }

    public static void w(Throwable tr, String msg, Object... args) {
        w(formatMessage(msg, args), tr);
    }

    public static void w(String msg) {
        instance().log(Level.WARNING, msg, null);
    }

    public static void w(String msg, Throwable tr) {
        instance().log(Level.WARNING, msg, tr);
    }

    public static void e(String msg, Object... args) {
        e(formatMessage(msg, args));
    }

    public static void e(Throwable tr, String msg, Object... args) {
        e(formatMessage(msg, args), tr);
    }

    public static void e(String msg) {
        instance().log(Level.ERROR, msg, null);
    }

    public static void e(String msg, Throwable tr) {
        instance().log(Level.ERROR, msg, tr);
    }

    public static void e(Throwable tr) {
        instance().log(Level.ERROR, "", tr);
    }

    public static void wtf(String msg, Object... args) {
        wtf(formatMessage(msg, args));
    }

    public static void wtf(String msg) {
        instance().log(Level.ASSERT, msg, null);
    }

    public static void wtf(String msg, Throwable tr) {
        instance().log(Level.ASSERT, msg, tr);
    }
}
