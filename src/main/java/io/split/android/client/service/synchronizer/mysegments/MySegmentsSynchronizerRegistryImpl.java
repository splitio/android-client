package io.split.android.client.service.synchronizer.mysegments;

import androidx.core.util.Consumer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MySegmentsSynchronizerRegistryImpl implements MySegmentsSynchronizerRegistry,
    MySegmentsSynchronizerRegistry.Tasks{

    private final AtomicBoolean mLoadedFromCache = new AtomicBoolean(false);
    private final AtomicBoolean mSynchronizedSegments = new AtomicBoolean(false);
    private final AtomicBoolean mScheduledSegmentsSyncTask = new AtomicBoolean(false);
    private final AtomicBoolean mStoppedPeriodicFetching = new AtomicBoolean(false);
    private final ConcurrentMap<String, MySegmentsSynchronizer> mMySegmentsSynchronizers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MySegmentsSynchronizer> mMyLargeSegmentsSynchronizers = new ConcurrentHashMap<>();

    @Override
    public synchronized void registerMySegmentsSynchronizer(String userKey, MySegmentsSynchronizer mySegmentsSynchronizer) {
        mMySegmentsSynchronizers.put(userKey, mySegmentsSynchronizer);
        triggerPendingActions(mySegmentsSynchronizer);
    }

    @Override
    public synchronized void registerMyLargeSegmentsSynchronizer(String userKey, MySegmentsSynchronizer mySegmentsSynchronizer) {
        mMyLargeSegmentsSynchronizers.put(userKey, mySegmentsSynchronizer);
        triggerPendingActions(mySegmentsSynchronizer);
    }

    @Override
    public synchronized void unregisterMySegmentsSynchronizer(String userKey) {
        MySegmentsSynchronizer mySegmentsSynchronizer = mMySegmentsSynchronizers.get(userKey);
        if (mySegmentsSynchronizer != null) {
            mySegmentsSynchronizer.stopPeriodicFetching();
            mySegmentsSynchronizer.destroy();
        }

        MySegmentsSynchronizer myLargeSegmentsSynchronizer = mMyLargeSegmentsSynchronizers.get(userKey);
        if (myLargeSegmentsSynchronizer != null) {
            myLargeSegmentsSynchronizer.stopPeriodicFetching();
            myLargeSegmentsSynchronizer.destroy();
        }

        mMySegmentsSynchronizers.remove(userKey);
    }

    @Override
    public synchronized void loadMySegmentsFromCache(SegmentType segmentType) {
        executeForAll(MySegmentsSynchronizer::loadMySegmentsFromCache, segmentType);

        mLoadedFromCache.set(true);
    }

    @Override
    public void synchronizeMySegments(SegmentType segmentType) {
        executeForAll(MySegmentsSynchronizer::synchronizeMySegments, segmentType);

        mSynchronizedSegments.set(true);
    }

    @Override
    public void forceMySegmentsSync(SegmentType segmentType) {
        executeForAll(MySegmentsSynchronizer::forceMySegmentsSync, segmentType);
    }

    @Override
    public synchronized void destroy() {
        executeForAll(MySegmentsSynchronizer::destroy);
    }

    @Override
    public synchronized void scheduleSegmentsSyncTask(SegmentType segmentType) {
        executeForAll(MySegmentsSynchronizer::scheduleSegmentsSyncTask, segmentType);

        mScheduledSegmentsSyncTask.set(true);
    }

    @Override
    public void submitMySegmentsLoadingTask(SegmentType segmentType) {
        executeForAll(MySegmentsSynchronizer::submitMySegmentsLoadingTask, segmentType);
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
        executeForAll(consumer, SegmentType.SEGMENT);
        executeForAll(consumer, SegmentType.LARGE_SEGMENT);
    }

    private void executeForAll(Consumer<MySegmentsSynchronizer> consumer, SegmentType segmentType) {
        ConcurrentMap<String, MySegmentsSynchronizer> synchronizers = segmentType == SegmentType.SEGMENT ?
                mMySegmentsSynchronizers : mMyLargeSegmentsSynchronizers;

        for (MySegmentsSynchronizer mySegmentsSynchronizer : synchronizers.values()) {
            consumer.accept(mySegmentsSynchronizer);
        }
    }
}
