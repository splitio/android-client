package io.split.android.client.service.sseclient.notifications.memberships;

import io.split.android.client.service.sseclient.notifications.MembershipNotification;

public interface MembershipsNotificationProcessor {

    void process(MembershipNotification notification);
}
