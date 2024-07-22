package io.split.android.client.service.sseclient.reactor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.utils.logger.Logger;

public class MySegmentsUpdateWorkerRegistryImpl implements MySegmentsUpdateWorkerRegistry {

    private final AtomicBoolean mStarted = new AtomicBoolean(false);
    private final ConcurrentMap<String, MySegmentsUpdateWorker> mMySegmentUpdateWorkers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MySegmentsUpdateWorker> mMyLargeSegmentUpdateWorkers = new ConcurrentHashMap<>();

    @Override
    public synchronized void registerMySegmentsUpdateWorker(String matchingKey, MySegmentsUpdateWorker mySegmentsUpdateWorker) {
        mMySegmentUpdateWorkers.put(matchingKey, mySegmentsUpdateWorker);
        startIfNeeded(mySegmentsUpdateWorker);
    }

    @Override
    public synchronized void registerMyLargeSegmentsUpdateWorker(String matchingKey, MySegmentsUpdateWorker mySegmentsUpdateWorker) {
        mMyLargeSegmentUpdateWorkers.put(matchingKey, mySegmentsUpdateWorker);
        startIfNeeded(mySegmentsUpdateWorker);
    }

    @Override
    public synchronized void unregisterMySegmentsUpdateWorker(String matchingKey) {
        MySegmentsUpdateWorker mySegmentsUpdateWorker = mMySegmentUpdateWorkers.get(matchingKey);
        if (mySegmentsUpdateWorker != null) {
            mySegmentsUpdateWorker.stop();
        }
        mMySegmentUpdateWorkers.remove(matchingKey);

        MySegmentsUpdateWorker myLargeSegmentsUpdateWorker = mMyLargeSegmentUpdateWorkers.get(matchingKey);
        if (myLargeSegmentsUpdateWorker != null) {
            myLargeSegmentsUpdateWorker.stop();
        }
        mMyLargeSegmentUpdateWorkers.remove(matchingKey);
    }

    @Override
    public void start() {
        if (!mStarted.getAndSet(true)) {
            if (mMySegmentUpdateWorkers.isEmpty()) {
                Logger.d("No MySegmentsUpdateWorkers have been registered");
            }

            for (MySegmentsUpdateWorker mySegmentsUpdateWorker : mMySegmentUpdateWorkers.values()) {
                mySegmentsUpdateWorker.start();
            }

            for (MySegmentsUpdateWorker mySegmentsUpdateWorker : mMyLargeSegmentUpdateWorkers.values()) {
                mySegmentsUpdateWorker.start();
            }
        }
    }

    @Override
    public void stop() {
        if (mStarted.getAndSet(false)) {
            for (MySegmentsUpdateWorker mySegmentsUpdateWorker : mMySegmentUpdateWorkers.values()) {
                mySegmentsUpdateWorker.stop();
            }

            for (MySegmentsUpdateWorker myLargeSegmentsUpdateWorker: mMyLargeSegmentUpdateWorkers.values()) {
                myLargeSegmentsUpdateWorker.stop();
            }
        }
    }

    private void startIfNeeded(MySegmentsUpdateWorker mySegmentsUpdateWorker) {
        if (mStarted.get()) {
            mySegmentsUpdateWorker.start();
        }
    }
}
