package io.split.android.client.service.sseclient.spi;

import androidx.annotation.NonNull;

import io.split.android.client.service.sseclient.notifications.IncomingNotification;

/**
 * Listener interface for update notifications from the streaming module.
 * Host applications implement this to handle split/RBS/kill/membership updates.
 */
public interface UpdateNotificationListener {

    /**
     * Called when an update notification is received.
     * The notification type can be checked to determine the specific update type:
     * - SPLIT_UPDATE
     * - SPLIT_KILL
     * - RULE_BASED_SEGMENT_UPDATE
     * - MEMBERSHIPS_MS_UPDATE
     * - MEMBERSHIPS_LS_UPDATE
     *
     * @param notification the incoming update notification
     */
    void onUpdateNotification(@NonNull IncomingNotification notification);
}
