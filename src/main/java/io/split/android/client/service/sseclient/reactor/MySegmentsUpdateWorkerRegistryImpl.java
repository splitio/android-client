package io.split.android.client.service.sseclient.reactor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.utils.Logger;

public class MySegmentsUpdateWorkerRegistryImpl implements MySegmentsUpdateWorkerRegistry {

    private final AtomicBoolean mStarted = new AtomicBoolean(false);
    private final ConcurrentMap<String, MySegmentsUpdateWorker> mMySegmentUpdateWorkers = new ConcurrentHashMap<>();

    @Override
    public synchronized void registerMySegmentsUpdateWorker(String matchingKey, MySegmentsUpdateWorker mySegmentsUpdateWorker) {
        mMySegmentUpdateWorkers.put(matchingKey, mySegmentsUpdateWorker);
        if (mStarted.get()) {
            mySegmentsUpdateWorker.start();
        }
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
    public synchronized void start() {
        if (!mStarted.get()) {
            if (mMySegmentUpdateWorkers.isEmpty()) {
                Logger.d("No MySegmentsUpdateWorkers have been registered");
            }

            for (MySegmentsUpdateWorker mySegmentsUpdateWorker : mMySegmentUpdateWorkers.values()) {
                mySegmentsUpdateWorker.start();
            }

            mStarted.set(true);
        }
    }

    @Override
    public synchronized void stop() {
        if (mStarted.get()) {
            for (MySegmentsUpdateWorker mySegmentsUpdateWorker : mMySegmentUpdateWorkers.values()) {
                mySegmentsUpdateWorker.stop();
            }

            mStarted.set(false);
        }
    }
}
