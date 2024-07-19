package io.split.android.client.service.sseclient.notifications.mysegments;

import androidx.annotation.NonNull;

public interface MySegmentsNotificationProcessorFactory {

    MySegmentsNotificationProcessor getProcessor(MySegmentsNotificationProcessorConfiguration configuration);

    MyLargeSegmentsNotificationProcessor getForLargeSegments(@NonNull MySegmentsNotificationProcessorConfiguration configuration);
}
