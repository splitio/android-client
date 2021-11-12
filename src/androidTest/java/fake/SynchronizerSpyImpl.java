package fake;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import io.split.android.client.dtos.Event;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.synchronizer.SynchronizerSpy;

public class SynchronizerSpyImpl implements SynchronizerSpy {

    Synchronizer mSynchronizer;
    public CountDownLatch startPeriodicFetchLatch = null;
    public CountDownLatch stopPeriodicFetchLatch = null;

    public AtomicInteger mForceMySegmentSyncCalledCount = new AtomicInteger(0);

    @Override
    public void setSynchronizer(Synchronizer synchronizer) {
        mSynchronizer = synchronizer;
    }

    @Override
    public void loadAndSynchronizeSplits() {
        mSynchronizer.loadAndSynchronizeSplits();
    }

    @Override
    public void loadSplitsFromCache() {
        mSynchronizer.loadSplitsFromCache();
    }

    @Override
    public void loadMySegmentsFromCache() {
        mSynchronizer.loadMySegmentsFromCache();
    }

    @Override
    public void loadAttributesFromCache() {
        mSynchronizer.loadAttributesFromCache();
    }

    @Override
    public void synchronizeSplits(long since) {
        mSynchronizer.synchronizeSplits();
    }

    @Override
    public void synchronizeSplits() {
        mSynchronizer.synchronizeSplits();
    }

    @Override
    public void synchronizeMySegments() {
        mSynchronizer.synchronizeMySegments();
    }

    @Override
    public void forceMySegmentsSync() {
        mSynchronizer.forceMySegmentsSync();
        mForceMySegmentSyncCalledCount.addAndGet(1);
    }

    @Override
    public void startPeriodicFetching() {
        mSynchronizer.startPeriodicFetching();
        if (startPeriodicFetchLatch != null) {
            startPeriodicFetchLatch.countDown();
        }
    }

    @Override
    public void stopPeriodicFetching() {
        mSynchronizer.stopPeriodicFetching();
        if (stopPeriodicFetchLatch != null) {
            stopPeriodicFetchLatch.countDown();
        }
    }

    @Override
    public void startPeriodicRecording() {
        mSynchronizer.startPeriodicRecording();
    }

    @Override
    public void stopPeriodicRecording() {
        mSynchronizer.stopPeriodicRecording();
    }

    @Override
    public void pushEvent(Event event) {
        mSynchronizer.pushEvent(event);
    }

    @Override
    public void pushImpression(Impression impression) {
        mSynchronizer.pushImpression(impression);
    }

    @Override
    public void flush() {
        mSynchronizer.flush();
    }

    @Override
    public void destroy() {
        mSynchronizer.destroy();
    }

    @Override
    public void pause() {
        mSynchronizer.pause();
    }

    @Override
    public void resume() {
        mSynchronizer.resume();
    }
}
