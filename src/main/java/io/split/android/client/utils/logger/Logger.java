package io.split.android.client.utils.logger;

import android.util.Log;

/**
 * Created by sarrubia on 2/20/18.
 */

public class Logger {

    private static final String TAG = "SplitSDK";
    private int mLevel = SplitLogLevel.NONE;
    private static Logger instance;
    private LogPrinter mLogPrinter = new LogPrinterImpl();

    private Logger(){}

    public static synchronized Logger instance(){
        if(instance == null){
            synchronized (Logger.class) { // double checked locking principle to improve performance
                if(instance == null){
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

    private void log(int priority, String msg, Throwable tr){

        if (priority < Log.VERBOSE ||
                mLevel < priority) {
            return;
        }

        switch (priority) {
            case Log.VERBOSE:
                mLogPrinter.v(TAG, msg, tr);
                break;

            case Log.DEBUG:
                mLogPrinter.d(TAG, msg, tr);
                break;

            case Log.INFO:
                mLogPrinter.i(TAG, msg, tr);
                break;

            case Log.WARN:
                mLogPrinter.w(TAG, msg, tr);
                break;

            case Log.ERROR:
                mLogPrinter.e(TAG, msg, tr);
                break;

            case Log.ASSERT:
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

    public static void v(String msg, Object... args){
        v(formatMessage(msg,args));
    }

    public static void v(String msg){
        instance().log(Log.VERBOSE, msg, null);
    }

    public static void v(String msg, Throwable tr){
        instance().log(Log.VERBOSE, msg, tr);
    }

    public static void d(String msg, Object... args){
        d(formatMessage(msg,args));
    }

    public static void d(String msg){
        instance().log(Log.DEBUG, msg, null);
    }

    public static void d(String msg, Throwable tr){
        instance().log(Log.DEBUG, msg, tr);
    }

    public static void i(String msg, Object... args){
        i(formatMessage(msg,args));
    }

    public static void i(String msg){
        instance().log(Log.INFO, msg, null);
    }

    public static void i(String msg, Throwable tr){
        instance().log(Log.INFO, msg, tr);
    }

    public static void w(String msg, Object... args){
        w(formatMessage(msg,args));
    }

    public static void w(Throwable tr, String msg, Object... args ){
        w(formatMessage(msg,args), tr);
    }

    public static void w(String msg){
        instance().log(Log.WARN, msg, null);
    }

    public static void w(String msg, Throwable tr){
        instance().log(Log.WARN, msg, tr);
    }

    public static void e(String msg, Object... args){
        e(formatMessage(msg,args));
    }

    public static void e(Throwable tr, String msg, Object... args ){
        e(formatMessage(msg,args), tr);
    }

    public static void e(String msg){
        instance().log(Log.ERROR, msg, null);
    }

    public static void e(String msg, Throwable tr){
        instance().log(Log.ERROR, msg, tr);
    }

    public static void e(Throwable tr){
        instance().log(Log.ERROR, "", tr);
    }

    public static void wtf(String msg, Object... args){
        wtf(formatMessage(msg,args));
    }

    public static void wtf(String msg){
        instance().log(Log.ASSERT, msg, null);
    }

    public static void wtf(String msg, Throwable tr){
        instance().log(Log.ASSERT, msg, tr);
    }
}
