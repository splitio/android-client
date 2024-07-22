package io.split.android.client.service.sseclient.notifications.mysegments;

public interface MySegmentsNotificationProcessorFactory {

    MySegmentsNotificationProcessor getProcessor(MySegmentsNotificationProcessorConfiguration configuration);

    MyLargeSegmentsNotificationProcessor getForLargeSegments(MySegmentsNotificationProcessorConfiguration configuration);
}
