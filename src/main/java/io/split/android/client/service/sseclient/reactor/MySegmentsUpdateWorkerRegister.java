package io.split.android.client.service.sseclient.reactor;

public interface MySegmentsUpdateWorkerRegister {

    void registerMySegmentsUpdateWorker(String matchingKey, MySegmentsUpdateWorker mySegmentsUpdateWorker);

    void unregisterMySegmentsUpdateWorker(String matchingKey);
}
