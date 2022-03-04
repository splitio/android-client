package io.split.android.client.service.sseclient.reactor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.split.android.client.utils.Logger;

public class MySegmentsUpdateWorkerRegistryImpl implements MySegmentsUpdateWorkerRegistry {

    private final ConcurrentMap<String, MySegmentsUpdateWorker> mMySegmentUpdateWorkers = new ConcurrentHashMap<>();

    @Override
    public void registerMySegmentsUpdateWorker(String matchingKey, MySegmentsUpdateWorker mySegmentsUpdateWorker) {
        mMySegmentUpdateWorkers.put(matchingKey, mySegmentsUpdateWorker);
    }

    @Override
    public void unregisterMySegmentsUpdateWorker(String matchingKey) {
        mMySegmentUpdateWorkers.remove(matchingKey);
    }

    @Override
    public void start() {
        if (mMySegmentUpdateWorkers.isEmpty()) {
            Logger.w("No MySegmentsUpdateWorkers have been registered");
        }

        for (MySegmentsUpdateWorker mySegmentsUpdateWorker : mMySegmentUpdateWorkers.values()) {
            mySegmentsUpdateWorker.start();
        }
    }

    @Override
    public void stop() {
        for (MySegmentsUpdateWorker mySegmentsUpdateWorker : mMySegmentUpdateWorkers.values()) {
            mySegmentsUpdateWorker.stop();
        }
    }
}
