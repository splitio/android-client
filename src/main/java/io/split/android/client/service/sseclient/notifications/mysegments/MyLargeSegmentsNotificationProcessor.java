package io.split.android.client.service.sseclient.notifications.mysegments;

import io.split.android.client.service.sseclient.notifications.MyLargeSegmentChangeNotification;

public interface MyLargeSegmentsNotificationProcessor {

    void process(MyLargeSegmentChangeNotification notification);
}
