package io.split.android.client.service.sseclient.notifications.mysegments;

import io.split.android.client.service.sseclient.notifications.MyLargeSegmentChangeNotification;

interface MyLargeSegmentsNotificationProcessor {

    void process(MyLargeSegmentChangeNotification notification);
}
