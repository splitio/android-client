package io.split.android.client;

import android.os.SystemClock;

import io.split.android.client.utils.logger.Logger;

public class TimeChecker {

    private static final long START_TIME_MS;

    static {
        START_TIME_MS = SystemClock.elapsedRealtime();
        Logger.v("[SPTPRF] Time checker started at: " + System.currentTimeMillis());
    }
    public static long getElapsedTime() {
        return SystemClock.elapsedRealtime() - START_TIME_MS;
    }

    public static void timeCheckerLog(String message, long elapsedRealtimeStart) {
        long elapsedTime = now() - elapsedRealtimeStart;
        Logger.v("[SPTPRF] " + System.currentTimeMillis() + " " + message + ": " + elapsedTime + " ms");
    }

    public static void timeSinceStartLog(String message) {
        long elapsedTime = getElapsedTime();
        Logger.v("[SPTPRF] " + System.currentTimeMillis() + " " + message + ": " + elapsedTime + " ms since start");
    }

    public static long now() {
        return SystemClock.elapsedRealtime();
    }
}
