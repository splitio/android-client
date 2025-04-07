package io.split.android.client;

import static io.split.android.client.utils.Utils.checkNotNull;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.storage.common.SplitStorageContainer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.telemetry.storage.TelemetryInitProducer;
import io.split.android.client.utils.logger.Logger;
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
    private final ImpressionListener.FederatedImpressionListener mCustomerImpressionListener;
    private final SplitValidatorImpl mSplitValidator;
    private final SplitFactoryImpl.EventsTrackerProvider mEventsTrackerProvider;

    public SplitClientFactoryImpl(@NonNull SplitFactory splitFactory,
                                  @NonNull SplitClientContainer clientContainer,
                                  @NonNull SplitClientConfig config,
                                  @NonNull SyncManager syncManager,
                                  @NonNull TelemetrySynchronizer telemetrySynchronizer,
                                  @NonNull SplitStorageContainer storageContainer,
                                  @NonNull SplitTaskExecutor splitTaskExecutor,
                                  @NonNull ValidationMessageLogger validationLogger,
                                  @NonNull KeyValidator keyValidator,
                                  @NonNull SplitFactoryImpl.EventsTrackerProvider eventsTrackerProvider,
                                  @NonNull ImpressionListener.FederatedImpressionListener customerImpressionListener,
                                  @Nullable FlagSetsFilter flagSetsFilter) {
        mSplitFactory = checkNotNull(splitFactory);
        mClientContainer = checkNotNull(clientContainer);
        mConfig = checkNotNull(config);
        mSyncManager = checkNotNull(syncManager);

        mStorageContainer = checkNotNull(storageContainer);
        mTelemetrySynchronizer = checkNotNull(telemetrySynchronizer);
        mCustomerImpressionListener = checkNotNull(customerImpressionListener);
        mEventsTrackerProvider = checkNotNull(eventsTrackerProvider);

        mAttributesManagerFactory = getAttributesManagerFactory(config.persistentAttributesEnabled(),
                validationLogger,
                splitTaskExecutor,
                mStorageContainer.getPersistentAttributesStorage());
        mSplitParser = new SplitParser(mStorageContainer.getMySegmentsStorageContainer(), mStorageContainer.getMyLargeSegmentsStorageContainer());
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
                mEventsTrackerProvider.getEventsTracker(),
                mAttributesManagerFactory.getManager(key.matchingKey(), attributesStorage),
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

        System.out.println("[DEBUG-SDK-STARTUP]Registering SDK_READY_FROM_CACHE event handler");
        eventsManager.register(SplitEvent.SDK_READY_FROM_CACHE, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                System.out.println("[DEBUG-SDK-STARTUP]SDK_READY_FROM_CACHE triggered, time elapsed: " + (System.currentTimeMillis() - initializationStartTime) + "ms");
                telemetryInitProducer.recordTimeUntilReadyFromCache(System.currentTimeMillis() - initializationStartTime);
                
                // Log the number of splits available
                try {
                    int splitCount = mSplitFactory.manager().splitNames().size();
                    System.out.println("[DEBUG-SDK-STARTUP]Number of splits loaded from cache: " + splitCount);
                } catch (Exception e) {
                    System.out.println("[DEBUG-SDK-STARTUP]Failed to get split count: " + e.getMessage());
                }
            }
        });

        System.out.println("[DEBUG-SDK-STARTUP]Registering SDK_READY event handler");
        eventsManager.register(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                System.out.println("[DEBUG-SDK-STARTUP]SDK_READY triggered, time elapsed: " + (System.currentTimeMillis() - initializationStartTime) + "ms");
                telemetryInitProducer.recordTimeUntilReady(System.currentTimeMillis() - initializationStartTime);
                telemetrySynchronizer.synchronizeConfig();
                
                // Log the number of splits available
                try {
                    int splitCount = mSplitFactory.manager().splitNames().size();
                    System.out.println("[DEBUG-SDK-STARTUP]Number of splits available after sync: " + splitCount);
                } catch (Exception e) {
                    System.out.println("[DEBUG-SDK-STARTUP]Failed to get split count: " + e.getMessage());
                }
            }
        });
    }
}
