package io.split.android.client.shared;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitClientFactory;
import io.split.android.client.SplitClientFactoryImpl;
import io.split.android.client.SplitFactoryImpl;
import io.split.android.client.api.Key;
import io.split.android.client.events.EventsManagerCoordinator;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.ValidationMessageLogger;

public class SplitClientContainerImpl implements SplitClientContainer {

    private final ConcurrentMap<String, SplitClient> mClientInstances = new ConcurrentHashMap<>();
    private final SplitClientFactory mSplitClientFactory;
    private final Object mClientCreationLock = new Object();

    public SplitClientContainerImpl(@NonNull SplitClientFactory splitClientFactory) {
        mSplitClientFactory = checkNotNull(splitClientFactory);
    }

    public SplitClientContainerImpl(@NonNull SplitFactoryImpl splitFactory,
                                    @NonNull SplitClientConfig config,
                                    @NonNull SyncManager mSyncManager,
                                    @NonNull Synchronizer mSynchronizer,
                                    @NonNull TelemetrySynchronizer telemetrySynchronizer,
                                    @NonNull EventsManagerCoordinator mEventsManagerCoordinator,
                                    @NonNull SplitStorageContainer mStorageContainer,
                                    @NonNull SplitTaskExecutor splitTaskExecutor,
                                    @NonNull SplitApiFacade mSplitApiFacade,
                                    @NonNull ValidationMessageLogger validationLogger,
                                    @NonNull KeyValidator keyValidator,
                                    @NonNull ImpressionListener customerImpressionListener) {
        mSplitClientFactory = new SplitClientFactoryImpl(splitFactory,
                this,
                config,
                mSyncManager,
                mSynchronizer,
                telemetrySynchronizer,
                mEventsManagerCoordinator,
                mStorageContainer,
                splitTaskExecutor,
                mSplitApiFacade,
                validationLogger,
                keyValidator,
                customerImpressionListener);
    }

    @Override
    public SplitClient getClient(Key key) {
        return getOrCreateClientForKey(key);
    }

    @Override
    public void remove(String key) {
        mClientInstances.remove(key);
    }

    private SplitClient getOrCreateClientForKey(Key key) {
        synchronized (mClientCreationLock) {
            if (mClientInstances.get(key.matchingKey()) != null) {
                return mClientInstances.get(key.matchingKey());
            }

            boolean isDefaultClient = mClientInstances.isEmpty();
            SplitClient newClient = mSplitClientFactory.getClient(key, isDefaultClient);

            mClientInstances.put(key.matchingKey(), newClient);
        }

        return mClientInstances.get(key.matchingKey());
    }

    @Override
    public Set<SplitClient> getAll() {
        return new HashSet<>(mClientInstances.values());
    }
}
