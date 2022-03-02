package io.split.android.client;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManagerFactory;
import io.split.android.client.attributes.AttributesManagerFactoryImpl;
import io.split.android.client.attributes.AttributesMergerImpl;
import io.split.android.client.events.EventsManagerRegister;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.attributes.AttributeTaskFactoryImpl;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentsTaskFactoryConfiguration;
import io.split.android.client.service.mysegments.MySegmentsTaskFactoryProvider;
import io.split.android.client.service.mysegments.MySegmentsTaskFactoryProviderImpl;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorkerRegistry;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizer;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerFactoryImpl;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegister;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizer;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerFactory;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerFactoryImpl;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegister;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
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
    private final SplitClientConfig mConfig;
    private final SyncManager mSyncManager;
    private final MySegmentsSynchronizerFactory mMySegmentsSynchronizerFactory;
    private final AttributesSynchronizerFactoryImpl mAttributesSynchronizerFactory;
    private final MySegmentsTaskFactoryProvider mMySegmentsTaskFactoryProvider;
    private final TelemetrySynchronizer mTelemetrySynchronizer;
    private final SplitApiFacade mSplitApiFacade;
    private final SplitStorageContainer mStorageContainer;
    private final SplitParser mSplitParser;
    private final AttributesManagerFactory mAttributesManagerFactory;
    private final TreatmentManagerFactory mTreatmentManagerFactory;
    private final EventsManagerRegister mEventsManagerRegister;
    private final MySegmentsSynchronizerRegister mMySegmentsSynchronizerRegister;
    private final AttributesSynchronizerRegister mAttributesSynchronizerRegister;
    private final MySegmentsUpdateWorkerRegistry mMySegmentsUpdateWorkerRegistry;
    private final ImpressionListener mCustomerImpressionListener;
    private final SplitValidatorImpl mSplitValidator;
    private final EventPropertiesProcessorImpl mEventPropertiesProcessor;

    public SplitClientFactoryImpl(@NonNull SplitFactory splitFactory,
                                  @NonNull SplitClientConfig config,
                                  @NonNull SyncManager syncManager,
                                  @NonNull Synchronizer synchronizer,
                                  @NonNull TelemetrySynchronizer telemetrySynchronizer,
                                  @NonNull EventsManagerRegister eventsManagerRegister,
                                  @NonNull SplitStorageContainer storageContainer,
                                  @NonNull SplitTaskExecutor splitTaskExecutor,
                                  @NonNull SplitApiFacade splitApiFacade,
                                  @NonNull ValidationMessageLogger validationLogger,
                                  @NonNull KeyValidator keyValidator,
                                  @NonNull ImpressionListener customerImpressionListener) {
        mSplitFactory = checkNotNull(splitFactory);
        mConfig = checkNotNull(config);
        mSyncManager = checkNotNull(syncManager);
        mEventsManagerRegister = checkNotNull(eventsManagerRegister);
        mStorageContainer = checkNotNull(storageContainer);
        mSplitApiFacade = checkNotNull(splitApiFacade);
        mTelemetrySynchronizer = checkNotNull(telemetrySynchronizer);
        mCustomerImpressionListener = checkNotNull(customerImpressionListener);
        mMySegmentsSynchronizerFactory = new MySegmentsSynchronizerFactoryImpl(new RetryBackoffCounterTimerFactory(),
                splitTaskExecutor,
                config.segmentsRefreshRate());
        mAttributesSynchronizerFactory = new AttributesSynchronizerFactoryImpl(splitTaskExecutor,
                mStorageContainer.getPersistentAttributesStorage());
        mMySegmentsTaskFactoryProvider = new MySegmentsTaskFactoryProviderImpl(mStorageContainer.getTelemetryStorage());
        mAttributesManagerFactory = getAttributesManagerFactory(config.persistentAttributesEnabled(),
                validationLogger,
                splitTaskExecutor,
                mStorageContainer.getPersistentAttributesStorage());
        mSplitParser = new SplitParser(mStorageContainer.getMySegmentsStorageContainer());
        mSplitValidator = new SplitValidatorImpl();
        mTreatmentManagerFactory = new TreatmentManagerFactoryImpl(
                keyValidator,
                mSplitValidator,
                customerImpressionListener,
                config.labelsEnabled(),
                new AttributesMergerImpl(),
                mStorageContainer.getTelemetryStorage(),
                new EvaluatorImpl(mStorageContainer.getSplitsStorage(), mSplitParser)
        );
        mMySegmentsSynchronizerRegister = (MySegmentsSynchronizerRegister) synchronizer;
        mAttributesSynchronizerRegister = (AttributesSynchronizerRegister) synchronizer;
        mMySegmentsUpdateWorkerRegistry = (MySegmentsUpdateWorkerRegistry) mSyncManager;
        mEventPropertiesProcessor = new EventPropertiesProcessorImpl();
    }

    @Override
    public SplitClient getClient(@NonNull Key key) {

        final long initializationStartTime = System.currentTimeMillis();

        MySegmentsStorage mySegmentsStorage = mStorageContainer.getMySegmentsStorage(key.matchingKey());

        SplitEventsManager eventsManager = new SplitEventsManager(mConfig);
        mEventsManagerRegister.registerEventsManager(key.matchingKey(), eventsManager);

        MySegmentsSynchronizer mySegmentsSynchronizer = mMySegmentsSynchronizerFactory.getSynchronizer(
                mMySegmentsTaskFactoryProvider.getFactory(
                        new MySegmentsTaskFactoryConfiguration(
                                mSplitApiFacade.getMySegmentsFetcher(key.matchingKey()),
                                mySegmentsStorage,
                                eventsManager
                        )
                ),
                eventsManager
        );

        AttributesStorage attributesStorage = mStorageContainer.getAttributesStorage(key.matchingKey());
        AttributesSynchronizer attributesSynchronizer = mAttributesSynchronizerFactory.getSynchronizer(
                new AttributeTaskFactoryImpl(
                        key.matchingKey(),
                        attributesStorage
                ),
                eventsManager
        );

        BlockingQueue<MySegmentChangeNotification> mySegmentChangeNotificationQueue = new LinkedBlockingDeque<>();

        MySegmentsUpdateWorker mySegmentUpdateWorker = new MySegmentsUpdateWorker(mySegmentsSynchronizer,
                mySegmentChangeNotificationQueue);
        mMySegmentsUpdateWorkerRegistry.registerMySegmentsUpdateWorker(key.matchingKey(), mySegmentUpdateWorker);

        mMySegmentsSynchronizerRegister.registerMySegmentsSynchronizer(key.matchingKey(),
                mySegmentsSynchronizer);

        mAttributesSynchronizerRegister.registerAttributesSynchronizer(key.matchingKey(),
                attributesSynchronizer);

        SplitClientImpl splitClient = new SplitClientImpl(mSplitFactory,
                key,
                mSplitParser,
                mCustomerImpressionListener,
                mConfig,
                eventsManager,
                mStorageContainer.getSplitsStorage(),
                mEventPropertiesProcessor,
                mSyncManager,
                mAttributesManagerFactory.getManager(key.matchingKey(), attributesStorage),
                mStorageContainer.getTelemetryStorage(),
                mSplitValidator,
                mTreatmentManagerFactory.getTreatmentManager(key,
                        eventsManager,
                        mAttributesManagerFactory.getManager(key.matchingKey(), attributesStorage)));

        eventsManager.getExecutorResources().setSplitClient(splitClient);

        registerTelemetryTasksInEventManager(eventsManager,
                mTelemetrySynchronizer,
                mStorageContainer.getTelemetryStorage(),
                initializationStartTime,
                mConfig.shouldRecordTelemetry());

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
