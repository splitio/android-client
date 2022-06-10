package io.split.android.client.shared;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitClientFactory;
import io.split.android.client.SplitClientFactoryImpl;
import io.split.android.client.SplitFactoryImpl;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.mysegments.MySegmentsTaskFactoryConfiguration;
import io.split.android.client.service.mysegments.MySegmentsTaskFactoryProvider;
import io.split.android.client.service.mysegments.MySegmentsTaskFactoryProviderImpl;
import io.split.android.client.service.sseclient.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.sseclient.PushNotificationManagerDeferredStartTask;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsBackgroundSyncScheduleTask;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsWorkManagerWrapper;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.ValidationMessageLogger;

public final class SplitClientContainerImpl extends BaseSplitClientContainer {

    private final String mDefaultMatchingKey;
    private final SplitClientFactory mSplitClientFactory;
    private final MySegmentsTaskFactoryProvider mMySegmentsTaskFactoryProvider;
    private final SplitApiFacade mSplitApiFacade;
    private final SplitStorageContainer mStorageContainer;
    private final SplitClientConfig mConfig;
    private final ClientComponentsRegister mClientComponentsRegister;
    private final PushNotificationManager mPushNotificationManager;
    private final boolean mStreamingEnabled;
    private final AtomicBoolean mConnecting = new AtomicBoolean(false);
    private final AtomicBoolean mSchedulingBackgroundSync = new AtomicBoolean(false);
    private final SplitTaskExecutor mSplitTaskExecutor;
    private SplitTaskExecutionListener mStreamingConnectionExecutionListener;
    private final SplitTaskExecutionListener mSchedulingBackgroundSyncExecutionListener;
    private final MySegmentsWorkManagerWrapper mWorkManagerWrapper;

    public SplitClientContainerImpl(@NonNull String defaultMatchingKey,
                                    @NonNull SplitFactoryImpl splitFactory,
                                    @NonNull SplitClientConfig config,
                                    @NonNull SyncManager syncManager,
                                    @NonNull TelemetrySynchronizer telemetrySynchronizer,
                                    @NonNull SplitStorageContainer storageContainer,
                                    @NonNull SplitTaskExecutor splitTaskExecutor,
                                    @NonNull SplitApiFacade splitApiFacade,
                                    @NonNull ValidationMessageLogger validationLogger,
                                    @NonNull KeyValidator keyValidator,
                                    @NonNull ImpressionListener customerImpressionListener,
                                    @Nullable PushNotificationManager pushNotificationManager,
                                    @NonNull ClientComponentsRegister clientComponentsRegister,
                                    @NonNull MySegmentsWorkManagerWrapper workManagerWrapper) {
        mDefaultMatchingKey = checkNotNull(defaultMatchingKey);
        mPushNotificationManager = pushNotificationManager;
        mStreamingEnabled = config.streamingEnabled();
        mMySegmentsTaskFactoryProvider = new MySegmentsTaskFactoryProviderImpl(storageContainer.getTelemetryStorage());
        mSplitApiFacade = checkNotNull(splitApiFacade);
        mStorageContainer = checkNotNull(storageContainer);
        mConfig = checkNotNull(config);
        mSplitClientFactory = new SplitClientFactoryImpl(splitFactory,
                this,
                config,
                syncManager,
                telemetrySynchronizer,
                storageContainer,
                splitTaskExecutor,
                validationLogger,
                keyValidator,
                customerImpressionListener
        );
        mClientComponentsRegister = checkNotNull(clientComponentsRegister);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mSchedulingBackgroundSyncExecutionListener = new WorkManagerSchedulingListener(mSchedulingBackgroundSync);
        mWorkManagerWrapper = checkNotNull(workManagerWrapper);

        // Avoid creating unnecessary components
        if (!config.singleSyncModeEnabled()) {
            mStreamingConnectionExecutionListener = new StreamingConnectionExecutionListener(mConnecting);
        }
    }

    @VisibleForTesting
    public SplitClientContainerImpl(String defaultMatchingKey,
                                    PushNotificationManager pushNotificationManager,
                                    boolean streamingEnabled,
                                    MySegmentsTaskFactoryProvider mySegmentsTaskFactoryProvider,
                                    SplitApiFacade splitApiFacade,
                                    SplitStorageContainer storageContainer,
                                    SplitTaskExecutor splitTaskExecutor,
                                    SplitClientConfig config,
                                    SplitClientFactory splitClientFactory,
                                    ClientComponentsRegister clientComponentsRegister,
                                    MySegmentsWorkManagerWrapper workManagerWrapper) {
        mDefaultMatchingKey = checkNotNull(defaultMatchingKey);
        mPushNotificationManager = pushNotificationManager;
        mStreamingEnabled = streamingEnabled;
        mMySegmentsTaskFactoryProvider = checkNotNull(mySegmentsTaskFactoryProvider);
        mSplitApiFacade = checkNotNull(splitApiFacade);
        mStorageContainer = checkNotNull(storageContainer);
        mConfig = checkNotNull(config);
        mSplitClientFactory = checkNotNull(splitClientFactory);
        mClientComponentsRegister = checkNotNull(clientComponentsRegister);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mStreamingConnectionExecutionListener = new StreamingConnectionExecutionListener(mConnecting);
        mSchedulingBackgroundSyncExecutionListener = new WorkManagerSchedulingListener(mConnecting);
        mWorkManagerWrapper = checkNotNull(workManagerWrapper);
    }

    @Override
    public void remove(Key key) {
        super.remove(key);
        mClientComponentsRegister.unregisterComponentsForKey(key);
    }

    @Override
    public void createNewClient(Key key) {
        SplitEventsManager eventsManager = new SplitEventsManager(mConfig);
        MySegmentsTaskFactory mySegmentsTaskFactory = getMySegmentsTaskFactory(key, eventsManager);

        SplitClient client = mSplitClientFactory.getClient(key, mySegmentsTaskFactory, eventsManager, mDefaultMatchingKey.equals(key.matchingKey()));
        trackNewClient(key, client);
        mClientComponentsRegister.registerComponents(key, mySegmentsTaskFactory, eventsManager);

        if (mStreamingEnabled && !mConfig.singleSyncModeEnabled()) {
            connectToStreaming();
        }
        if (mConfig.synchronizeInBackground()) {
            scheduleMySegmentsWork();
        } else {
            mWorkManagerWrapper.removeWork();
        }
    }

    private MySegmentsTaskFactory getMySegmentsTaskFactory(Key key, SplitEventsManager eventsManager) {
        return mMySegmentsTaskFactoryProvider.getFactory(
                new MySegmentsTaskFactoryConfiguration(
                        mSplitApiFacade.getMySegmentsFetcher(key.matchingKey()),
                        mStorageContainer.getMySegmentsStorage(key.matchingKey()),
                        eventsManager
                )
        );
    }

    private void connectToStreaming() {
        if (mConfig.singleSyncModeEnabled()) {
            return;
        }
        if (!mConnecting.getAndSet(true)) {
            mSplitTaskExecutor.schedule(new PushNotificationManagerDeferredStartTask(mPushNotificationManager),
                    ServiceConstants.MIN_INITIAL_DELAY,
                    mStreamingConnectionExecutionListener);
        }
    }

    private void scheduleMySegmentsWork() {
        if (!mSchedulingBackgroundSync.getAndSet(true)) {
            mSplitTaskExecutor.schedule(new MySegmentsBackgroundSyncScheduleTask(mWorkManagerWrapper, getKeySet()),
                    ServiceConstants.MIN_INITIAL_DELAY,
                    mSchedulingBackgroundSyncExecutionListener);
        }
    }

    @VisibleForTesting
    static class StreamingConnectionExecutionListener implements SplitTaskExecutionListener {

        private final AtomicBoolean mConnecting;

        StreamingConnectionExecutionListener(AtomicBoolean connecting) {
            mConnecting = connecting;
        }

        @Override
        public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
            mConnecting.set(false);
        }
    }

    @VisibleForTesting
    static class WorkManagerSchedulingListener implements SplitTaskExecutionListener {

        private final AtomicBoolean mScheduling;

        WorkManagerSchedulingListener(AtomicBoolean connecting) {
            mScheduling = connecting;
        }

        @Override
        public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
            mScheduling.set(false);
        }
    }
}
