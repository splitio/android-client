package io.split.android.client;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManagerFactory;
import io.split.android.client.attributes.AttributesManagerFactoryImpl;
import io.split.android.client.attributes.AttributesMergerImpl;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.storage.common.SplitStorageContainer;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.telemetry.storage.TelemetryInitProducer;
import io.split.android.client.validators.AttributesValidatorImpl;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.TreatmentManagerFactory;
import io.split.android.client.validators.TreatmentManagerFactoryImpl;
import io.split.android.client.validators.ValidationMessageLogger;
import io.split.android.engine.experiments.SplitParser;

public class SplitClientFactoryImpl implements SplitClientFactory {

    private final SplitFactory mSplitFactory;
    private final SplitClientContainer mClientContainer;
    private final SplitClientConfig mConfig;
    private final SyncManager mSyncManager;

    private final TelemetrySynchronizer mTelemetrySynchronizer;
    private final SplitStorageContainer mStorageContainer;
    private final SplitParser mSplitParser;
    private final AttributesManagerFactory mAttributesManagerFactory;
    private final TreatmentManagerFactory mTreatmentManagerFactory;
    private final ImpressionListener mCustomerImpressionListener;
    private final SplitValidatorImpl mSplitValidator;
    private final EventsTracker mEventsTracker;

    public SplitClientFactoryImpl(@NonNull SplitFactory splitFactory,
                                  @NonNull SplitClientContainer clientContainer,
                                  @NonNull SplitClientConfig config,
                                  @NonNull SyncManager syncManager,
                                  @NonNull TelemetrySynchronizer telemetrySynchronizer,
                                  @NonNull SplitStorageContainer storageContainer,
                                  @NonNull SplitTaskExecutor splitTaskExecutor,
                                  @NonNull ValidationMessageLogger validationLogger,
                                  @NonNull KeyValidator keyValidator,
                                  @NonNull EventsTracker eventsTracker,
                                  @NonNull ImpressionListener customerImpressionListener,
                                  @Nullable FlagSetsFilter flagSetsFilter) {
        mSplitFactory = checkNotNull(splitFactory);
        mClientContainer = checkNotNull(clientContainer);
        mConfig = checkNotNull(config);
        mSyncManager = checkNotNull(syncManager);

        mStorageContainer = checkNotNull(storageContainer);
        mTelemetrySynchronizer = checkNotNull(telemetrySynchronizer);
        mCustomerImpressionListener = checkNotNull(customerImpressionListener);
        mEventsTracker = checkNotNull(eventsTracker);

        mAttributesManagerFactory = getAttributesManagerFactory(config.persistentAttributesEnabled(),
                validationLogger,
                splitTaskExecutor,
                mStorageContainer.getPersistentAttributesStorage());
        mSplitParser = new SplitParser(mStorageContainer.getMySegmentsStorageContainer());
        mSplitValidator = new SplitValidatorImpl();
        SplitsStorage splitsStorage = mStorageContainer.getSplitsStorage();
        mTreatmentManagerFactory = new TreatmentManagerFactoryImpl(
                keyValidator,
                mSplitValidator,
                customerImpressionListener,
                config.labelsEnabled(),
                new AttributesMergerImpl(),
                mStorageContainer.getTelemetryStorage(),
                mSplitParser,
                flagSetsFilter,
                splitsStorage
        );
    }

    @Override
    public SplitClient getClient(@NonNull Key key,
                                 @NonNull MySegmentsTaskFactory mySegmentsTaskFactory,
                                 @NonNull SplitEventsManager eventsManager,
                                 boolean isDefaultClient) {
        final long initializationStartTime = System.currentTimeMillis();


        AttributesStorage attributesStorage = mStorageContainer.getAttributesStorage(key.matchingKey());

        SplitClientImpl splitClient = new SplitClientImpl(mSplitFactory,
                mClientContainer,
                key,
                mSplitParser,
                mCustomerImpressionListener,
                mConfig,
                eventsManager,
                mEventsTracker,
                mAttributesManagerFactory.getManager(key.matchingKey(), attributesStorage),
                mStorageContainer.getTelemetryStorage(),
                mSplitValidator,
                mTreatmentManagerFactory.getTreatmentManager(key,
                        eventsManager,
                        mAttributesManagerFactory.getManager(key.matchingKey(), attributesStorage)));

        eventsManager.getExecutorResources().setSplitClient(splitClient);

        if (isDefaultClient) {
            registerTelemetryTasksInEventManager(eventsManager,
                    mTelemetrySynchronizer,
                    mStorageContainer.getTelemetryStorage(),
                    initializationStartTime,
                    mConfig.shouldRecordTelemetry());
        }
        return splitClient;
    }

    private AttributesManagerFactory getAttributesManagerFactory(boolean persistentAttributesEnabled,
                                                                 ValidationMessageLogger validationLogger,
                                                                 SplitTaskExecutor _splitTaskExecutor,
                                                                 PersistentAttributesStorage persistentAttributesStorage) {
        if (persistentAttributesEnabled) {
            return new AttributesManagerFactoryImpl(new AttributesValidatorImpl(),
                    validationLogger,
                    persistentAttributesStorage,
                    _splitTaskExecutor);
        } else {
            return new AttributesManagerFactoryImpl(new AttributesValidatorImpl(), validationLogger);
        }
    }

    private void registerTelemetryTasksInEventManager(SplitEventsManager eventsManager,
                                                      TelemetrySynchronizer telemetrySynchronizer,
                                                      TelemetryInitProducer telemetryInitProducer,
                                                      long initializationStartTime,
                                                      boolean shouldRecordTelemetry) {
        if (!shouldRecordTelemetry) {
            return;
        }

        eventsManager.register(SplitEvent.SDK_READY_FROM_CACHE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                telemetryInitProducer.recordTimeUntilReadyFromCache(System.currentTimeMillis() - initializationStartTime);
            }
        });

        eventsManager.register(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                telemetryInitProducer.recordTimeUntilReady(System.currentTimeMillis() - initializationStartTime);
                telemetrySynchronizer.synchronizeConfig();
            }
        });
    }
}
