package io.split.android.client.utils;

import android.util.Log;

/**
 * Created by sarrubia on 2/20/18.
 */

public class Logger {

    private static final String TAG = "SplitSDK";
    private boolean _debugLevel = false;
    private static Logger instance;

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

    public synchronized void debugLevel(boolean enabled) {
        _debugLevel = enabled;
    }

    private void log(int priority, String msg, Throwable tr){
        switch (priority) {
            case Log.VERBOSE:
                if(_debugLevel) {
                    Log.v(TAG, msg, tr);
                }
                break;

            case Log.DEBUG:
                if(_debugLevel) {
                    Log.d(TAG, msg, tr);
                }
                break;

            case Log.INFO:
                Log.i(TAG, msg, tr);
                break;

            case Log.WARN:
                Log.w(TAG, msg, tr);
                break;

            case Log.ERROR:
                Log.e(TAG, msg, tr);
                break;

            case Log.ASSERT:
                Log.wtf(TAG, msg, tr);
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
