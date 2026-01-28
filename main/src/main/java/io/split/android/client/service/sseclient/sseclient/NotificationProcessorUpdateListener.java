package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;

import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.spi.UpdateNotificationListener;

/**
 * Adapter that forwards update notifications to NotificationProcessor.
 */
public class NotificationProcessorUpdateListener implements UpdateNotificationListener {

    private final NotificationProcessor mNotificationProcessor;

    public NotificationProcessorUpdateListener(@NonNull NotificationProcessor notificationProcessor) {
        mNotificationProcessor = notificationProcessor;
    }

    @Override
    public void onUpdateNotification(@NonNull IncomingNotification notification) {
        mNotificationProcessor.process(notification);
    }
}
