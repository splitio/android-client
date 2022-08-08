package io.split.android.client.service.sseclient.notifications.mysegments;

public interface MySegmentsNotificationProcessorRegistry {

    void registerMySegmentsProcessor(String matchingKey, MySegmentsNotificationProcessor processor);

    void unregisterMySegmentsProcessor(String matchingKey);
}
