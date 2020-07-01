package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.gson.JsonSyntaxException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.lifecycle.SplitLifecycleAware;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.notifications.ControlNotification;
import io.split.android.client.service.sseclient.notifications.IncomingNotification;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.NotificationProcessor;
import io.split.android.client.service.sseclient.notifications.OccupancyNotification;
import io.split.android.client.utils.Logger;

import static androidx.core.util.Preconditions.checkNotNull;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.DISABLE_POLLING;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.ENABLE_POLLING;
import static io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType.STREAMING_CONNECTED;
import static java.lang.reflect.Modifier.PRIVATE;

public class PushNotificationManager implements SseClientListener, SplitLifecycleAware, SseConnectionManagerListener {

    private final static String PRIMARY_CONTROL_CHANNEL = "control_pri";
    private final static String DATA_FIELD = "data";

    private final SseClient mSseClient;
    private final PushManagerEventBroadcaster mPushManagerEventBroadcaster;
    private final NotificationParser mNotificationParser;
    private final NotificationProcessor mNotificationProcessor;


    private AtomicBoolean mIsPollingEnabled;
    private AtomicLong mLastControlNotificationTime;
    private AtomicBoolean mIsStreamingPaused;
    private AtomicBoolean mIsHostAppInBackground;
    private AtomicBoolean mIsStopped;
    private SseConnectionManager mSseConnectionManager;

    public PushNotificationManager(@NonNull SseClient sseClient,
                                   @NonNull NotificationParser notificationParser,
                                   @NonNull NotificationProcessor notificationProcessor,
                                   @NonNull PushManagerEventBroadcaster pushManagerEventBroadcaster,
                                   @NonNull SseConnectionManager sseConnectionManager) {

        mSseClient = checkNotNull(sseClient);
        mNotificationParser = checkNotNull(notificationParser);
        mNotificationProcessor = checkNotNull(notificationProcessor);
        mPushManagerEventBroadcaster = checkNotNull(pushManagerEventBroadcaster);
        mSseConnectionManager = checkNotNull(sseConnectionManager);
        mIsPollingEnabled = new AtomicBoolean(false);
        mLastControlNotificationTime = new AtomicLong(0L);
        mIsStreamingPaused = new AtomicBoolean(false);
        mIsHostAppInBackground = new AtomicBoolean(false);
        mIsStopped = new AtomicBoolean(false);
        mSseClient.setListener(this);
        mSseConnectionManager.setListener(this);
    }

    public void start() {
        mSseConnectionManager.start();
    }

    public void stop() {
        mIsStopped.set(true);
        mSseConnectionManager.stop();
    }

    @Override
    public void pause() {
        if (mIsStopped.get()) {
            return;
        }
        mSseConnectionManager.pause();
        mIsHostAppInBackground.set(true);
    }

    @Override
    public void resume() {
        if (mIsStopped.get()) {
            return;
        }
        mSseConnectionManager.resume();
    }

    @VisibleForTesting(otherwise = PRIVATE)
    public void notifyPollingDisabled() {
        Logger.i("Sending polling disabled message through event broadcaster.");
        if (mIsPollingEnabled.getAndSet(false)) {
            mPushManagerEventBroadcaster.pushMessage(new PushStatusEvent(DISABLE_POLLING));
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    public void notifyPollingEnabled() {
        if (!mIsPollingEnabled.getAndSet(true)) {
            Logger.i("Enabling polling");
            mPushManagerEventBroadcaster.pushMessage(new PushStatusEvent(ENABLE_POLLING));
        }
    }

    public void notifyStreamingConnected() {
        Logger.i("Disabling polling");
        mPushManagerEventBroadcaster.pushMessage(new PushStatusEvent(STREAMING_CONNECTED));
    }

    // SSE Connection manager listener
    @Override
    public void onSseAvailable() {
        if (mIsStopped.get()) {
            return;
        }
        notifyPollingDisabled();
        notifyStreamingConnected();
    }

    @Override
    public void onSseNotAvailable() {
        notifyPollingEnabled();
    }

    //
//     SSE client listener implementation
//
    @Override
    public void onOpen() {
    }

    @Override
    public void onMessage(Map<String, String> values) {
        if (mIsStopped.get()) {
            return;
        }
        String messageData = values.get(DATA_FIELD);

        if (messageData != null) {
            IncomingNotification incomingNotification
                    = mNotificationParser.parseIncoming(messageData);
            if (incomingNotification == null) {
                return;
            }

            switch (incomingNotification.getType()) {
                case ERROR:
                    shutdownStreaming();
                    break;
                case CONTROL:
                    processControlNotification(incomingNotification);
                    break;
                case OCCUPANCY:
                    processOccupancyNotification(incomingNotification);
                    break;
                default:
                    if (!mIsStreamingPaused.get()) {
                        mNotificationProcessor.process(incomingNotification);
                    }
            }
        }
    }

    @Override
    public void onKeepAlive() {
    }

    @Override
    public void onError(boolean isRecoverable) {
    }

    @Override
    public void onDisconnect() {
    }

    private void processControlNotification(IncomingNotification incomingNotification) {
        try {
            if (mLastControlNotificationTime.get() > incomingNotification.getTimestamp()) {
                return;
            }

            mLastControlNotificationTime.set(incomingNotification.getTimestamp());
            ControlNotification controlNotification
                    = mNotificationParser.parseControl(incomingNotification.getJsonData());
            switch (controlNotification.getControlType()) {
                case STREAMING_DISABLED:
                    mIsStreamingPaused.set(true);
                    shutdownStreaming();
                    break;
                case STREAMING_ENABLED:
                    mIsStreamingPaused.set(false);
                    notifyPollingDisabled();
                    notifyStreamingConnected();
                    break;
                case STREAMING_PAUSED:
                    mIsStreamingPaused.set(true);
                    notifyPollingEnabled();
                    break;
                default:
                    Logger.e("Unknown message received" + controlNotification.getControlType());
            }
        } catch (JsonSyntaxException e) {
            Logger.e("Could not parse control notification: "
                    + incomingNotification.getJsonData() + " -> " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unexpected error while processing control notification: " +
                    e.getLocalizedMessage());
        }
    }

    private void shutdownStreaming() {
        mSseConnectionManager.stop();
        notifyPollingEnabled();
    }

    private void processOccupancyNotification(IncomingNotification incomingNotification) {

        // Using only primary control channel for now
        if (!incomingNotification.getChannel().contains(PRIMARY_CONTROL_CHANNEL)) {
            Logger.d("Ignoring occupancy notification in channel: "
                    + incomingNotification.getChannel());
            return;
        }

        try {
            OccupancyNotification occupancyNotification
                    = mNotificationParser.parseOccupancy(incomingNotification.getJsonData());
            if (occupancyNotification.getMetrics().getPublishers() < 1) {
                Logger.i("No publishers available for streaming channel: "
                        + incomingNotification.getChannel());
                notifyPollingEnabled();
            } else {
                notifyPollingDisabled();
            }
        } catch (JsonSyntaxException e) {
            Logger.e("Could not parse occupancy notification: "
                    + incomingNotification.getJsonData() + " -> " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("Unexpected error while processing occupancy notification: " +
                    e.getLocalizedMessage());
        }
    }
}
