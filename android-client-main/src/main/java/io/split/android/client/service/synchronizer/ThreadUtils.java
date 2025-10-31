package io.split.android.client.service.synchronizer;

import android.os.Handler;
import android.os.Looper;

public class ThreadUtils {
    public static boolean isCurrentThreadMain() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static void runInMainThread(Runnable runnable) {
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        mainThreadHandler.post(runnable);
    }
}