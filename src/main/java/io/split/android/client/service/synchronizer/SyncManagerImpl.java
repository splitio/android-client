package io.split.android.client.service.synchronizer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.feedbackchannel.BroadcastedEventListener;
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

    private AtomicBoolean isPollingEnabled;
    private PushManagerEventBroadcaster mPushManagerEventBroadcaster;
    private MySegmentsUpdateWorkerRegistry mMySegmentsUpdateWorkerRegistry;
    private PushNotificationManager mPushNotificationManager;
    private SplitUpdatesWorker mSplitUpdateWorker;
    private BackoffCounterTimer mStreamingReconnectTimer;

    public SyncManagerImpl(@NonNull SplitClientConfig splitClientConfig,
                           @NonNull Synchronizer synchronizer,
                           @Nullable PushNotificationManager pushNotificationManager,
                           @Nullable SplitUpdatesWorker splitUpdateWorker,
                           @Nullable PushManagerEventBroadcaster pushManagerEventBroadcaster,
                           @Nullable BackoffCounterTimer streamingReconnectTimer,
                           @NonNull TelemetrySynchronizer telemetrySynchronizer) {

        mSynchronizer = checkNotNull(synchronizer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mTelemetrySynchronizer = checkNotNull(telemetrySynchronizer);
        mIsPaused = new AtomicBoolean(false);
        isPollingEnabled = new AtomicBoolean(false);

        if (isSyncEnabled()) {
            mPushNotificationManager = pushNotificationManager;
            mSplitUpdateWorker = splitUpdateWorker;
            mPushManagerEventBroadcaster = pushManagerEventBroadcaster;
            mStreamingReconnectTimer = streamingReconnectTimer;
            mMySegmentsUpdateWorkerRegistry = new MySegmentsUpdateWorkerRegistryImpl();
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

        isPollingEnabled.set(!mSplitClientConfig.streamingEnabled());
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
            }
            if (isPollingEnabled.get()) {
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
            }
            if (isPollingEnabled.get()) {
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
    public void pushImpression(Impression impression) {
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
                isPollingEnabled.set(false);
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
                if(!mIsPaused.get()) {
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
                if(!mIsPaused.get()) {
                    mStreamingReconnectTimer.schedule();
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

    private boolean isSyncEnabled() {
        return mSplitClientConfig.syncEnabled();
    }

    private void enablePolling() {

        if (!isSyncEnabled()) {
            return;
        }

        if (!isPollingEnabled.get()) {
            isPollingEnabled.set(true);
            mSynchronizer.startPeriodicFetching();
            Logger.i("Polling enabled.");
        }
    }
}
