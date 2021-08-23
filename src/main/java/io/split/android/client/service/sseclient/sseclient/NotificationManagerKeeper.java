package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;

import com.google.common.collect.Maps;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType;
import io.split.android.client.service.sseclient.notifications.ControlNotification;
import io.split.android.client.service.sseclient.notifications.OccupancyNotification;
import io.split.android.client.utils.Logger;

public class NotificationManagerKeeper {

    private static class Publisher {
        int count = 0;
        long lastTimestamp;

        public Publisher(int count, long lastTimestamp) {
            this.count = count;
            this.lastTimestamp = lastTimestamp;
        }
    }

    private static String CHANNEL_PRI_KEY = "PRI";
    private static String CHANNEL_SEC_KEY = "SEC";

    Map<String, Publisher> mPublishers = Maps.newConcurrentMap();
    private PushManagerEventBroadcaster mBroadcasterChannel;
    private AtomicLong mLastControlTimestamp = new AtomicLong(0);
    private AtomicBoolean mIsStreamingActive = new AtomicBoolean(true);

    public NotificationManagerKeeper(PushManagerEventBroadcaster broadcasterChannel) {
        mBroadcasterChannel = broadcasterChannel;
        /// By default we consider one publisher en primary channel available
        mPublishers.put(CHANNEL_PRI_KEY, new Publisher(1, 0));
        mPublishers.put(CHANNEL_SEC_KEY, new Publisher(0, 0));
    }

    public void handleControlNotification(ControlNotification notification) {
        if(mLastControlTimestamp.get() >= notification.getTimestamp()) {
            return;
        }
        mLastControlTimestamp.set(notification.getTimestamp());
        try {
            switch (notification.getControlType()) {
                case STREAMING_PAUSED:
                    mIsStreamingActive.set(false);
                    mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_SUBSYSTEM_DOWN));
                    break;

                case STREAMING_DISABLED:
                    mIsStreamingActive.set(false);
                    mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_DISABLED));
                    break;

                case STREAMING_ENABLED:
                    mIsStreamingActive.set(true);
                    if (publishersCount() > 0) {
                        mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_SUBSYSTEM_UP));
                    }
                    break;

                case STREAMING_RESET:
                    if (publishersCount() > 0) {
                        mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_RESET));
                    }

                default:
                    Logger.e("Unknown message received" + notification.getControlType());
            }
        } catch (JsonSyntaxException e) {
            Logger.e("Could not parse control notification: "
                    + notification.getJsonData() + " -> " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unexpected error while processing control notification: " +
                    e.getLocalizedMessage());
        }
    }

    public void handleOccupancyNotification(OccupancyNotification notification) {
        String channelKey = getChannelKey(notification);

        if(channelKey == null || isOldTimestamp(notification, channelKey)) {
            return;
        }
        int prevPublishersCount = publishersCount();
        updateChannelInfo(channelKey, notification.getMetrics().getPublishers(), notification.getTimestamp());

        if (publishersCount() == 0 && prevPublishersCount > 0) {
            mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_SUBSYSTEM_DOWN));
            return;
        }

        if (publishersCount() > 0 && prevPublishersCount == 0 && mIsStreamingActive.get()) {
            mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_SUBSYSTEM_UP));
            return;
        }
    }

    public boolean isStreamingActive() {
        return mIsStreamingActive.get();
    }

    private synchronized int publishersCount() {
        return mPublishers.get(CHANNEL_PRI_KEY).count +  mPublishers.get(CHANNEL_SEC_KEY).count;
    }

    private @Nullable  String getChannelKey(OccupancyNotification notification ) {
        if (notification.isControlPriChannel()) {
            return CHANNEL_PRI_KEY;
        } else if (notification.isControlSecChannel()) {
            return CHANNEL_SEC_KEY;
        } else {
            Logger.w("Unknown occupancy channel " +  notification.getChannel());
            return null;
        }
    }

    private synchronized boolean isOldTimestamp(@NonNull OccupancyNotification notification, @NonNull String channelKey) {
        long timestamp = 0;
            timestamp = mPublishers.get(channelKey).lastTimestamp;

        return timestamp >= notification.getTimestamp();
    }

    private synchronized void updateChannelInfo( String channelKey, int publishersCount, long timestamp) {
        Publisher publisher = mPublishers.get(channelKey);
        if(publisher == null) {
            return;
        }
        publisher.lastTimestamp = timestamp;
        publisher.count = publishersCount;
    }
}
