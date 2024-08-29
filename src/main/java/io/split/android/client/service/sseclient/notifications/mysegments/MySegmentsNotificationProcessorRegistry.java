package io.split.android.client.service.sseclient.notifications.mysegments;

import io.split.android.client.service.sseclient.notifications.memberships.MembershipsNotificationProcessor;

public interface MySegmentsNotificationProcessorRegistry {

    void registerMembershipsNotificationProcessor(String matchingKey, MembershipsNotificationProcessor processor);

    void unregisterMembershipsProcessor(String matchingKey);
}
