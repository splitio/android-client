package io.split.android.client.service.sseclient.reactor;

public interface MySegmentsUpdateWorkerRegistry {

    void registerMySegmentsUpdateWorker(String matchingKey, MySegmentsUpdateWorker mySegmentsUpdateWorker);

    void unregisterMySegmentsUpdateWorker(String matchingKey);

    void start();

    void stop();
}
