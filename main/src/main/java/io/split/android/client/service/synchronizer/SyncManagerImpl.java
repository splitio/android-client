package io.split.android.client.service.synchronizer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.impressions.DecoratedImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.feedbackchannel.BroadcastedEventListener;
import io.split.android.client.service.sseclient.feedbackchannel.DelayStatusEvent;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorkerRegistry;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorkerRegistryImpl;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;
import io.split.android.client.service.sseclient.sseclient.BackoffCounterTimer;
import io.split.android.client.service.sseclient.sseclient.PushNotificationManager;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.utils.logger.Logger;

public class SyncManagerImpl implements SyncManager, BroadcastedEventListener, MySegmentsUpdateWorkerRegistry {

    private final SplitClientConfig mSplitClientConfig;
    private final Synchronizer mSynchronizer;
    private final AtomicBoolean mIsPaused;
    private final TelemetrySynchronizer mTelemetrySynchronizer;

    private final AtomicBoolean mIsPollingEnabled;
    @Nullable
    private final PushManagerEventBroadcaster mPushManagerEventBroadcaster;
    @Nullable
    private final MySegmentsUpdateWorkerRegistry mMySegmentsUpdateWorkerRegistry;
    @Nullable
    private final PushNotificationManager mPushNotificationManager;
    @Nullable
    private final SplitUpdatesWorker mSplitUpdateWorker;
    @Nullable
    private final BackoffCounterTimer mStreamingReconnectTimer;
    @Nullable
    private final SyncGuardian mSyncGuardian;

    public SyncManagerImpl(@NonNull SplitClientConfig splitClientConfig,
                           @NonNull Synchronizer synchronizer,
                           @Nullable PushNotificationManager pushNotificationManager,
                           @Nullable SplitUpdatesWorker splitUpdateWorker,
                           @Nullable PushManagerEventBroadcaster pushManagerEventBroadcaster,
                           @Nullable BackoffCounterTimer streamingReconnectTimer,
                           @Nullable SyncGuardian syncGuardian,
                           @NonNull TelemetrySynchronizer telemetrySynchronizer) {

        mSynchronizer = checkNotNull(synchronizer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mTelemetrySynchronizer = checkNotNull(telemetrySynchronizer);
        mIsPaused = new AtomicBoolean(false);
        mIsPollingEnabled = new AtomicBoolean(false);

        if (isSyncEnabled()) {
            mPushNotificationManager = pushNotificationManager;
            mSplitUpdateWorker = splitUpdateWorker;
            mPushManagerEventBroadcaster = pushManagerEventBroadcaster;
            mStreamingReconnectTimer = streamingReconnectTimer;
            mMySegmentsUpdateWorkerRegistry = new MySegmentsUpdateWorkerRegistryImpl();
            mSyncGuardian = syncGuardian;
        } else {
            mPushNotificationManager = null;
            mSplitUpdateWorker = null;
            mPushManagerEventBroadcaster = null;
            mStreamingReconnectTimer = null;
            mMySegmentsUpdateWorkerRegistry = null;
            mSyncGuardian = null;
        }
    }

    @Override
    public void start() {
        mSynchronizer.loadAndSynchronizeSplits();
        mSynchronizer.loadMySegmentsFromCache();
        mSynchronizer.loadAttributesFromCache();
        mSynchronizer.synchronizeMySegments();
        if (mSplitClientConfig.userConsent() == UserConsent.GRANTED) {
            Logger.v("User consent granted. Recording started");
            mSynchronizer.startPeriodicRecording();
        }
        mTelemetrySynchronizer.synchronizeStats();

        if (!isSyncEnabled()) {
            return;
        }

        mIsPollingEnabled.set(!mSplitClientConfig.streamingEnabled());
        if (mSplitClientConfig.streamingEnabled()) {
            mPushManagerEventBroadcaster.register(this);
            mSplitUpdateWorker.start();
            mMySegmentsUpdateWorkerRegistry.start();
            mStreamingReconnectTimer.setTask(new SplitTask() {
                @NonNull
                @Override
                public SplitTaskExecutionInfo execute() {
                    Logger.d("Reconnecting to streaming");
                    mPushNotificationManager.start();
                    return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
                }
            });

        } else {
            mSynchronizer.startPeriodicFetching();
        }
    }

    @Override
    public void pause() {
        mIsPaused.set(true);
        mSynchronizer.pause();
        mTelemetrySynchronizer.flush();
        if (isSyncEnabled()) {
            if (mSplitClientConfig.streamingEnabled()) {
                mPushNotificationManager.pause();
                if (mSyncGuardian != null) {
                    mSyncGuardian.initialize();
                }
            }
            if (mIsPollingEnabled.get()) {
                mSynchronizer.stopPeriodicFetching();
            }
        }
    }

    @Override
    public void resume() {
        mIsPaused.set(false);
        mSynchronizer.resume();

        if (isSyncEnabled()) {
            if (mSplitClientConfig.streamingEnabled()) {
                mPushNotificationManager.resume();
                triggerFeatureFlagsSyncIfNeeded();
            }

            if (mIsPollingEnabled.get()) {
                mSynchronizer.startPeriodicFetching();
            }
        }
    }

    @Override
    public void flush() {
        mSynchronizer.flush();
        mTelemetrySynchronizer.flush();
    }

    @Override
    public void pushEvent(Event event) {
        mSynchronizer.pushEvent(event);
    }

    @Override
    public void pushImpression(DecoratedImpression impression) {
        mSynchronizer.pushImpression(impression);
    }

    @Override
    public void stop() {
        if (mSplitClientConfig.userConsent() == UserConsent.GRANTED) {
            mSynchronizer.stopPeriodicRecording();
        }
        mSynchronizer.destroy();
        mTelemetrySynchronizer.destroy();
        if (isSyncEnabled()) {
            mPushNotificationManager.stop();
            mSplitUpdateWorker.stop();
            mMySegmentsUpdateWorkerRegistry.stop();
            mSynchronizer.stopPeriodicFetching();
        }
    }

    @Override
    public void onEvent(PushStatusEvent message) {
        if (!isSyncEnabled()) {
            return;
        }

        switch (message.getMessage()) {
            case PUSH_SUBSYSTEM_UP:
                Logger.d("Push Subsystem Up event message received.");
                mSynchronizer.synchronizeSplits();
                mSynchronizer.synchronizeMySegments();
                mSynchronizer.stopPeriodicFetching();
                mStreamingReconnectTimer.cancel();
                mIsPollingEnabled.set(false);
                break;

            case PUSH_SUBSYSTEM_DOWN:
                Logger.d("Push Subsystem Down event message received.");
                enablePolling();
                mStreamingReconnectTimer.cancel();
                break;

            case PUSH_RETRYABLE_ERROR:
                Logger.d("Push Subsystem recoverable error received.");
                enablePolling();
                // If sdk is paused (host app in bg) push manager should reconnect on resume
                if (!mIsPaused.get()) {
                    mStreamingReconnectTimer.schedule();
                }
                break;

            case PUSH_NON_RETRYABLE_ERROR:
                Logger.d("Push Subsystem non recoverable error received.");
                enablePolling();
                mStreamingReconnectTimer.cancel();
                mPushNotificationManager.stop();
                break;

            case PUSH_DISABLED:
                Logger.d("Push Subsystem Down event message received.");
                enablePolling();
                mStreamingReconnectTimer.cancel();
                mPushNotificationManager.stop();
                break;

            case PUSH_RESET:
                Logger.d("Push Subsystem reset received.");
                // If sdk is paused (host app in bg) push manager should reconnect on resume
                mPushNotificationManager.disconnect();
                if (!mIsPaused.get()) {
                    mStreamingReconnectTimer.schedule();
                }
                break;

            case SUCCESSFUL_SYNC:
                if (mSyncGuardian != null) {
                    Logger.v("Successful sync event received, updating last sync timestamp");
                    mSyncGuardian.updateLastSyncTimestamp();
                }
                break;

            case PUSH_DELAY_RECEIVED:
                try {
                    DelayStatusEvent delayEvent = (DelayStatusEvent) message;
                    if (mSyncGuardian != null) {
                        Logger.v("Streaming delay event received");
                        mSyncGuardian.setMaxSyncPeriod(delayEvent.getDelay());
                    }
                } catch (ClassCastException ex) {
                    Logger.w("Invalid streaming delay event received");
                }
                break;

            default:
                Logger.e("Invalid SSE event received: " + message.getMessage());
        }
    }

    @Override
    public void registerMySegmentsUpdateWorker(String matchingKey, MySegmentsUpdateWorker mySegmentsUpdateWorker) {
        mMySegmentsUpdateWorkerRegistry.registerMySegmentsUpdateWorker(matchingKey, mySegmentsUpdateWorker);
    }

    @Override
    public void unregisterMySegmentsUpdateWorker(String matchingKey) {
        mMySegmentsUpdateWorkerRegistry.unregisterMySegmentsUpdateWorker(matchingKey);
    }

    @Override
    public void setupUserConsent(UserConsent status) {
        if (status == UserConsent.GRANTED) {
            Logger.v("User consent status is granted now. Starting recorders");
            mSynchronizer.startPeriodicRecording();
        } else {
            Logger.v("User consent status is " + status + " now. Stopping recorders");
            mSynchronizer.stopPeriodicRecording();
        }
    }

    private boolean isSyncEnabled() {
        return mSplitClientConfig.syncEnabled();
    }

    private void enablePolling() {
        if (!isSyncEnabled()) {
            return;
        }

        if (!mIsPollingEnabled.get()) {
            mIsPollingEnabled.set(true);
            mSynchronizer.startPeriodicFetching();
            Logger.i("Polling enabled.");
        }
    }

    private void triggerFeatureFlagsSyncIfNeeded() {
        if (mPushNotificationManager.isSseClientDisconnected()) {
            if (mSyncGuardian.mustSync()) {
                Logger.v("Must sync, synchronizing splits");
                mSynchronizer.synchronizeSplits();
            } else {
                Logger.v("No need to sync");
            }
        } else {
            Logger.v("SSE client is connected, no need to trigger sync");
        }
    }
}
