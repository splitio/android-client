package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private AtomicBoolean isStreamingPaused;

    public SseHandler(@NonNull NotificationParser notificationParser,
                      @NonNull NotificationProcessor notificationProcessor,
                      @NonNull NotificationManagerKeeper notificationManagerKeeper,
                      @NonNull PushManagerEventBroadcaster broadcasterChannel) {

        mNotificationParser = checkNotNull(notificationParser);
        mNotificationProcessor = checkNotNull(notificationProcessor);
        mBroadcasterChannel = checkNotNull(broadcasterChannel);
        mNotificationManagerKeeper = checkNotNull(notificationManagerKeeper);
        isStreamingPaused = new AtomicBoolean(false);
    }

    public void handleIncomingMessage(Map<String, String> values) {

        String messageData = values.get(DATA_FIELD);

        if (messageData != null) {
            if(mNotificationParser.isError(values)) {
                handleError(messageData);
                return;
            }

            IncomingNotification incomingNotification = mNotificationParser.parseIncoming(messageData);
            if (incomingNotification == null) {
                return;
            }

            switch (incomingNotification.getType()) {
                case CONTROL:
                    handleControlNotification(incomingNotification);
                    break;
                case OCCUPANCY:
                    handleOccupancyNotification(incomingNotification);
                    break;
                case SPLIT_KILL:
                case SPLIT_UPDATE:
                case MY_SEGMENTS_UPDATE:
                    if(!isStreamingPaused.get()) {
                        mNotificationProcessor.process(incomingNotification);
                    }
                    break;
                default:
                    Logger.w("SSE Handler: Unknown notification");
            }
        }
    }

    public void handleError(boolean retryable) {
        PushStatusEvent event = new PushStatusEvent(retryable ? EventType.PUSH_RETRYABLE_ERROR : EventType.PUSH_NON_RETRYABLE_ERROR);
        mBroadcasterChannel.pushMessage(event);
    }

    private void handleControlNotification(IncomingNotification incomingNotification) {
        try {
            ControlNotification notification = mNotificationParser.parseControl(incomingNotification.getJsonData());
            notification.setTimestamp(incomingNotification.getTimestamp());
            // If push disabled also set isPaused = true to avoid process any notification until push shuts down
            isStreamingPaused.set(!(notification.getControlType() == ControlNotification.ControlType.STREAMING_ENABLED));
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

    private void handleError(String jsonData) {

        try {
            StreamingError errorNotification = mNotificationParser.parseError(jsonData);
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
                    + jsonData + " -> " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unexpected error while processing occupancy notification: " +
                    e.getLocalizedMessage());
        }
    }
}
