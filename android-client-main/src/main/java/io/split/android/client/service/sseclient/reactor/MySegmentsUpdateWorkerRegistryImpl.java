package io.split.android.client.service.sseclient.reactor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.utils.logger.Logger;

public class MySegmentsUpdateWorkerRegistryImpl implements MySegmentsUpdateWorkerRegistry {

    private final AtomicBoolean mStarted = new AtomicBoolean(false);
    private final ConcurrentMap<String, MySegmentsUpdateWorker> mMySegmentUpdateWorkers = new ConcurrentHashMap<>();

    @Override
    public synchronized void registerMySegmentsUpdateWorker(String matchingKey, MySegmentsUpdateWorker mySegmentsUpdateWorker) {
        mMySegmentUpdateWorkers.put(matchingKey, mySegmentsUpdateWorker);
        startIfNeeded(mySegmentsUpdateWorker);
    }

    @Override
    public synchronized void unregisterMySegmentsUpdateWorker(String matchingKey) {
        MySegmentsUpdateWorker mySegmentsUpdateWorker = mMySegmentUpdateWorkers.get(matchingKey);
        if (mySegmentsUpdateWorker != null) {
            mySegmentsUpdateWorker.stop();
        }
        mMySegmentUpdateWorkers.remove(matchingKey);
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
        }
    }

    @Override
    public void stop() {
        if (mStarted.getAndSet(false)) {
            for (MySegmentsUpdateWorker mySegmentsUpdateWorker : mMySegmentUpdateWorkers.values()) {
                mySegmentsUpdateWorker.stop();
            }
        }
    }

    private void startIfNeeded(MySegmentsUpdateWorker mySegmentsUpdateWorker) {
        if (mStarted.get()) {
            mySegmentsUpdateWorker.start();
        }
    }
}
