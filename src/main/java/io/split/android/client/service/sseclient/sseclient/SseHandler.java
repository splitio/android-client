package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;

import java.util.Map;

import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType;
import io.split.android.client.service.sseclient.notifications.ControlNotification;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.OccupancyNotification;
import io.split.android.client.service.sseclient.notifications.StreamingError;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SseHandler {
    private static final String DATA_FIELD = "data";

    private final PushManagerEventBroadcaster mBroadcasterChannel;
    private final NotificationParser mNotificationParser;
    private final NotificationProcessor mNotificationProcessor;
    private final NotificationManagerKeeper mNotificationManagerKeeper;

    public SseHandler(@NonNull NotificationParser notificationParser,
                      @NonNull NotificationProcessor notificationProcessor,
                      @NonNull NotificationManagerKeeper notificationManagerKeeper,
                      @NonNull PushManagerEventBroadcaster broadcasterChannel) {

        mNotificationParser = checkNotNull(notificationParser);
        mNotificationProcessor = checkNotNull(notificationProcessor);
        mBroadcasterChannel = checkNotNull(broadcasterChannel);
        mNotificationManagerKeeper = checkNotNull(notificationManagerKeeper);
    }

    public void handleIncomingMessage(Map<String, String> values) {

        String messageData = values.get(DATA_FIELD);

        if (messageData != null) {
            IncomingNotification incomingNotification
                    = mNotificationParser.parseIncoming(messageData);
            if (incomingNotification == null) {
                return;
            }

            switch (incomingNotification.getType()) {
                case ERROR:
                    handleError(incomingNotification);
                    break;
                case CONTROL:
                    handleControlNotification(incomingNotification);
                    break;
                case OCCUPANCY:
                    handleOccupancyNotification(incomingNotification);
                    break;
                case SPLIT_KILL:
                case SPLIT_UPDATE:
                case MY_SEGMENTS_UPDATE:
                    mNotificationProcessor.process(incomingNotification);
                default:
                    Logger.w("SSE Handler: Unknown notification");
            }
        }
    }

    public void reportError(boolean retryable) {
        PushStatusEvent event = new PushStatusEvent(retryable ? EventType.PUSH_RETRYABLE_ERROR : EventType.PUSH_NON_RETRYABLE_ERROR);
        mBroadcasterChannel.pushMessage(event);
    }

    private void handleControlNotification(IncomingNotification incomingNotification) {
        try {
            ControlNotification notification = mNotificationParser.parseControl(incomingNotification.getJsonData());
            notification.setTimestamp(incomingNotification.getTimestamp());
            mNotificationManagerKeeper.handleControlNotification(notification);
        } catch (JsonSyntaxException e) {
            Logger.e("Could not parse control notification: "
                    + incomingNotification.getJsonData() + " -> " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unexpected error while processing control notification: " +
                    e.getLocalizedMessage());
        }
    }

    private void handleOccupancyNotification(IncomingNotification incomingNotification) {

        try {
            OccupancyNotification notification = mNotificationParser.parseOccupancy(incomingNotification.getJsonData());
            notification.setChannel(incomingNotification.getChannel());
            notification.setTimestamp(incomingNotification.getTimestamp());
            mNotificationManagerKeeper.handleOccupancyNotification(notification);
        } catch (JsonSyntaxException e) {
            Logger.e("Could not parse occupancy notification: "
                    + incomingNotification.getJsonData() + " -> " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unexpected error while processing occupancy notification: " +
                    e.getLocalizedMessage());
        }
    }

    private void handleError(IncomingNotification incomingNotification) {

        try {
            StreamingError errorNotification = mNotificationParser.parseError(incomingNotification.getJsonData());
            Logger.w("Streaming error notification received: " + errorNotification.getMessage());
            if(errorNotification.shouldBeIgnored()) {
                Logger.w("Error ignored");
                return;
            }

            PushStatusEvent message = new PushStatusEvent(
                    errorNotification.isRetryable() ? EventType.PUSH_RETRYABLE_ERROR : EventType.PUSH_NON_RETRYABLE_ERROR);
            mBroadcasterChannel.pushMessage(message);

        } catch (JsonSyntaxException e) {
            Logger.e("Could not parse occupancy notification: "
                    + incomingNotification.getJsonData() + " -> " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unexpected error while processing occupancy notification: " +
                    e.getLocalizedMessage());
        }
    }
}
