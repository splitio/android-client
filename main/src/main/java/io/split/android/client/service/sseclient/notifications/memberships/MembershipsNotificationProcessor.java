package io.split.android.client.service.sseclient.notifications.memberships;

import androidx.annotation.Nullable;

import io.split.android.client.service.sseclient.notifications.MembershipNotification;

public interface MembershipsNotificationProcessor {

    void process(@Nullable MembershipNotification notification);
}
