package io.split.android.client.service.sseclient.notifications.mysegments;

import io.split.android.client.service.sseclient.notifications.memberships.MembershipsNotificationProcessor;

public interface MembershipsNotificationProcessorFactory {

    MembershipsNotificationProcessor getProcessor(MySegmentsNotificationProcessorConfiguration configuration);
}
