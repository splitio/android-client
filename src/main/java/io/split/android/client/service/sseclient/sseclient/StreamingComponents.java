package io.split.android.client.service.sseclient.sseclient;

import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;

public class StreamingComponents {
    private PushNotificationManager pushNotificationManager;
    private BlockingQueue<SplitsChangeNotification> splitsUpdateNotificationQueue;
    private PushManagerEventBroadcaster pushManagerEventBroadcaster;
    private NotificationParser notificationParser;
    private NotificationProcessor notificationProcessor;
    private SseAuthenticator sseAuthenticator;

    public StreamingComponents() {
    }

    public StreamingComponents(PushNotificationManager pushNotificationManager,
                               BlockingQueue<SplitsChangeNotification> splitsUpdateNotificationQueue,
                               NotificationParser notificationParser,
                               NotificationProcessor notificationProcessor,
                               SseAuthenticator sseAuthenticator,
                               PushManagerEventBroadcaster pushManagerEventBroadcaster) {
        this.pushNotificationManager = pushNotificationManager;
        this.splitsUpdateNotificationQueue = splitsUpdateNotificationQueue;
        this.notificationParser = notificationParser;
        this.notificationProcessor = notificationProcessor;
        this.sseAuthenticator = sseAuthenticator;
        this.pushManagerEventBroadcaster = pushManagerEventBroadcaster;
    }

    public PushNotificationManager getPushNotificationManager() {
        return pushNotificationManager;
    }

    public BlockingQueue<SplitsChangeNotification> getSplitsUpdateNotificationQueue() {
        return splitsUpdateNotificationQueue;
    }

    public PushManagerEventBroadcaster getPushManagerEventBroadcaster() {
        return pushManagerEventBroadcaster;
    }

    public NotificationParser getNotificationParser() {
        return notificationParser;
    }

    public NotificationProcessor getNotificationProcessor() {
        return notificationProcessor;
    }

    public SseAuthenticator getSseAuthenticator() {
        return sseAuthenticator;
    }
}
