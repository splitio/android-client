package io.split.android.client.service.sseclient.notifications.mysegments;

import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeV2Notification;

public interface MySegmentsNotificationProcessor {

    void processMySegmentsUpdate(MySegmentChangeNotification notification);

    void processMySegmentsUpdateV2(MySegmentChangeV2Notification notification);
}
