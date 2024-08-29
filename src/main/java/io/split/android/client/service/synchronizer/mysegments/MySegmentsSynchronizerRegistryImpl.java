package io.split.android.client.service.synchronizer.mysegments;

import androidx.core.util.Consumer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.service.mysegments.MySegmentUpdateParams;

public class MySegmentsSynchronizerRegistryImpl implements MySegmentsSynchronizerRegistry,
    MySegmentsSynchronizer {

    private final AtomicBoolean mLoadedFromCache = new AtomicBoolean(false);
    private final AtomicBoolean mSynchronizedSegments = new AtomicBoolean(false);
    private final AtomicBoolean mScheduledSegmentsSyncTask = new AtomicBoolean(false);
    private final AtomicBoolean mStoppedPeriodicFetching = new AtomicBoolean(false);
    private final ConcurrentMap<String, MySegmentsSynchronizer> mMySegmentsSynchronizers = new ConcurrentHashMap<>();

    @Override
    public synchronized void registerMySegmentsSynchronizer(String userKey, MySegmentsSynchronizer mySegmentsSynchronizer) {
        mMySegmentsSynchronizers.put(userKey, mySegmentsSynchronizer);
        triggerPendingActions(mySegmentsSynchronizer);
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
        executeForAll(MySegmentsSynchronizer::loadMySegmentsFromCache);

        mLoadedFromCache.set(true);
    }

    @Override
    public void synchronizeMySegments() {
        executeForAll(MySegmentsSynchronizer::synchronizeMySegments);

        mSynchronizedSegments.set(true);
    }

    @Override
    public void forceMySegmentsSync(MySegmentUpdateParams params) {
        executeForAll(mySegmentsSynchronizer -> mySegmentsSynchronizer.forceMySegmentsSync(params));
    }

    @Override
    public synchronized void destroy() {
        executeForAll(MySegmentsSynchronizer::destroy);
    }

    @Override
    public synchronized void scheduleSegmentsSyncTask() {
        executeForAll(MySegmentsSynchronizer::scheduleSegmentsSyncTask);

        mScheduledSegmentsSyncTask.set(true);
    }

    @Override
    public void submitMySegmentsLoadingTask() {
        executeForAll(MySegmentsSynchronizer::submitMySegmentsLoadingTask);
    }

    @Override
    public synchronized void stopPeriodicFetching() {
        executeForAll(MySegmentsSynchronizer::stopPeriodicFetching);

        mScheduledSegmentsSyncTask.set(false);
        mStoppedPeriodicFetching.set(true);
    }

    private void triggerPendingActions(MySegmentsSynchronizer mySegmentsSynchronizer) {
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

    private void executeForAll(Consumer<MySegmentsSynchronizer> consumer) {
        for (MySegmentsSynchronizer mySegmentsSynchronizer : mMySegmentsSynchronizers.values()) {
            consumer.accept(mySegmentsSynchronizer);
        }
    }
}
