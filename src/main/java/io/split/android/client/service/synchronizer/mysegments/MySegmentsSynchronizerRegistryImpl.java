package io.split.android.client.service.synchronizer.mysegments;

import androidx.annotation.VisibleForTesting;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MySegmentsSynchronizerRegistryImpl implements MySegmentsSynchronizerRegistry, MySegmentsSynchronizer {

    private final AtomicBoolean mLoadedFromCache = new AtomicBoolean(false);
    private final AtomicBoolean mSynchronizedSegments = new AtomicBoolean(false);
    private final AtomicBoolean mScheduledSegmentsSyncTask = new AtomicBoolean(false);
    private final AtomicBoolean mStoppedPeriodicFetching = new AtomicBoolean(false);
    private final ConcurrentMap<String, MySegmentsSynchronizer> mMySegmentsSynchronizers = new ConcurrentHashMap<>();

    @Override
    public synchronized void registerMySegmentsSynchronizer(String userKey, MySegmentsSynchronizer mySegmentsSynchronizer) {
        mMySegmentsSynchronizers.put(userKey, mySegmentsSynchronizer);
        if (mLoadedFromCache.get()) {
            mySegmentsSynchronizer.loadMySegmentsFromCache();
        }

        if (mSynchronizedSegments.get()) {
            mySegmentsSynchronizer.synchronizeMySegments();
        }

        if (mScheduledSegmentsSyncTask.get()) {
            mySegmentsSynchronizer.scheduleSegmentsSyncTask();
        }
    }

    @Override
    public synchronized void unregisterMySegmentsSynchronizer(String userKey) {
        MySegmentsSynchronizer mySegmentsSynchronizer = mMySegmentsSynchronizers.get(userKey);
        if (mySegmentsSynchronizer != null) {
            mySegmentsSynchronizer.stopPeriodicFetching();
            mySegmentsSynchronizer.destroy();
        }
        mMySegmentsSynchronizers.remove(userKey);
    }

    @Override
    public synchronized void loadMySegmentsFromCache() {
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            mySegmentsSynchronizer.loadMySegmentsFromCache();
        }

        mLoadedFromCache.set(true);
    }

    @Override
    public void synchronizeMySegments() {
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            mySegmentsSynchronizer.synchronizeMySegments();
        }

        mSynchronizedSegments.set(true);
    }

    @Override
    @VisibleForTesting
    public void forceMySegmentsSync() {
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            mySegmentsSynchronizer.forceMySegmentsSync();
        }
    }

    @Override
    public synchronized void destroy() {
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            mySegmentsSynchronizer.destroy();
        }
    }

    @Override
    public synchronized void scheduleSegmentsSyncTask() {
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            mySegmentsSynchronizer.scheduleSegmentsSyncTask();
        }

        mScheduledSegmentsSyncTask.set(true);
    }

    @Override
    public void submitMySegmentsLoadingTask() {
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            mySegmentsSynchronizer.submitMySegmentsLoadingTask();
        }
    }

    @Override
    public synchronized void stopPeriodicFetching() {
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            mySegmentsSynchronizer.stopPeriodicFetching();
        }

        mScheduledSegmentsSyncTask.set(false);
        mStoppedPeriodicFetching.set(true);
    }
}
