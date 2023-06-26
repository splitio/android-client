package io.split.android.client.service.sseclient.sseclient;

import java.util.concurrent.BlockingQueue;

import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.SplitsChangeNotification;
import io.split.android.client.service.synchronizer.SyncGuardian;

public class StreamingComponents {

    private PushNotificationManager mPushNotificationManager;
    private BlockingQueue<SplitsChangeNotification> mSplitsUpdateNotificationQueue;
    private PushManagerEventBroadcaster mPushManagerEventBroadcaster;
    private NotificationParser mNotificationParser;
    private NotificationProcessor mNotificationProcessor;
    private SseAuthenticator mSseAuthenticator;
    private SyncGuardian mSyncGuardian;

    public StreamingComponents() {
    }

    public StreamingComponents(PushNotificationManager pushNotificationManager,
                               BlockingQueue<SplitsChangeNotification> splitsUpdateNotificationQueue,
                               NotificationParser notificationParser,
                               NotificationProcessor notificationProcessor,
                               SseAuthenticator sseAuthenticator,
                               PushManagerEventBroadcaster pushManagerEventBroadcaster,
                               SyncGuardian syncManager) {
        mPushNotificationManager = pushNotificationManager;
        mSplitsUpdateNotificationQueue = splitsUpdateNotificationQueue;
        mNotificationParser = notificationParser;
        mNotificationProcessor = notificationProcessor;
        mSseAuthenticator = sseAuthenticator;
        mPushManagerEventBroadcaster = pushManagerEventBroadcaster;
        mSyncGuardian = syncManager;
    }

    public PushNotificationManager getPushNotificationManager() {
        return mPushNotificationManager;
    }

    public BlockingQueue<SplitsChangeNotification> getSplitsUpdateNotificationQueue() {
        return mSplitsUpdateNotificationQueue;
    }

    public PushManagerEventBroadcaster getPushManagerEventBroadcaster() {
        return mPushManagerEventBroadcaster;
    }

    public NotificationParser getNotificationParser() {
        return mNotificationParser;
    }

    public NotificationProcessor getNotificationProcessor() {
        return mNotificationProcessor;
    }

    public SseAuthenticator getSseAuthenticator() {
        return mSseAuthenticator;
    }

    public SyncGuardian getSyncGuardian() {
        return mSyncGuardian;
    }
}
