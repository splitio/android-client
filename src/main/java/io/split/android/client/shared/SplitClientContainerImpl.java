package io.split.android.client.shared;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.ValidationMessageLogger;

public class SplitClientContainerImpl implements SplitClientContainer {

    private final String mDefaultMatchingKey;
    private final ConcurrentMap<String, SplitClient> mClientInstances = new ConcurrentHashMap<>();
    private final SplitClientFactory mSplitClientFactory;
    private final Object mClientCreationLock = new Object();
    private final MySegmentsTaskFactoryProvider mMySegmentsTaskFactoryProvider;
    private final SplitApiFacade mSplitApiFacade;
    private final SplitStorageContainer mStorageContainer;
    private final SplitClientConfig mConfig;
    private final ClientComponentsRegister mClientComponentsRegister;
    private final PushNotificationManager mPushNotificationManager;
    private final boolean mStreamingEnabled;
    private final AtomicBoolean mConnecting = new AtomicBoolean(false);
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitTaskExecutionListener mStreamingConnectionExecutionListener;

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
                                    @NonNull PushNotificationManager pushNotificationManager,
                                    @NonNull ClientComponentsRegister clientComponentsRegister) {
        mDefaultMatchingKey = checkNotNull(defaultMatchingKey);
        mPushNotificationManager = checkNotNull(pushNotificationManager);
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
        mStreamingConnectionExecutionListener = new StreamingConnectionExecutionListener(mConnecting);
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
                                    ClientComponentsRegister clientComponentsRegister) {
        mDefaultMatchingKey = checkNotNull(defaultMatchingKey);
        mPushNotificationManager = checkNotNull(pushNotificationManager);
        mStreamingEnabled = streamingEnabled;
        mMySegmentsTaskFactoryProvider = checkNotNull(mySegmentsTaskFactoryProvider);
        mSplitApiFacade = checkNotNull(splitApiFacade);
        mStorageContainer = checkNotNull(storageContainer);
        mConfig = checkNotNull(config);
        mSplitClientFactory = checkNotNull(splitClientFactory);
        mClientComponentsRegister = checkNotNull(clientComponentsRegister);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mStreamingConnectionExecutionListener = new StreamingConnectionExecutionListener(mConnecting);
    }

    @Override
    public SplitClient getClient(Key key) {
        return getOrCreateClientForKey(key);
    }

    @Override
    public void remove(String key) {
        mClientInstances.remove(key);
        mClientComponentsRegister.unregisterComponentsForKey(key);
    }

    @Override
    public Set<SplitClient> getAll() {
        return new HashSet<>(mClientInstances.values());
    }

    private SplitClient getOrCreateClientForKey(Key key) {
        synchronized (mClientCreationLock) {
            if (mClientInstances.get(key.matchingKey()) != null) {
                return mClientInstances.get(key.matchingKey());
            }

            createNewClient(key);
        }

        return mClientInstances.get(key.matchingKey());
    }

    private void createNewClient(Key key) {
        SplitEventsManager eventsManager = new SplitEventsManager(mConfig);
        MySegmentsTaskFactory mySegmentsTaskFactory = getMySegmentsTaskFactory(key, eventsManager);

        SplitClient client = mSplitClientFactory.getClient(key, mySegmentsTaskFactory, eventsManager, mDefaultMatchingKey.equals(key.matchingKey()));
        mClientInstances.put(key.matchingKey(), client);

        mClientComponentsRegister.registerComponents(key, mySegmentsTaskFactory, eventsManager);

        if (mStreamingEnabled) {
            connectToStreaming();
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
        if (!mConnecting.getAndSet(true)) {
            mSplitTaskExecutor.schedule(new PushNotificationManagerDeferredStartTask(mPushNotificationManager),
                    ServiceConstants.DEFAULT_INITIAL_DELAY,
                    mStreamingConnectionExecutionListener);
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
}
