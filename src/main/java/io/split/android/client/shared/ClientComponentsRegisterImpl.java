package io.split.android.client.shared;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.LinkedBlockingDeque;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.api.Key;
import io.split.android.client.events.EventsManagerRegistry;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.attributes.AttributeTaskFactoryImpl;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.sseclient.notifications.MySegmentChangeNotification;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessor;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessorConfiguration;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessorFactory;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessorRegistry;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorkerRegistry;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticator;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizer;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerFactory;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegistry;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizer;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerFactory;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistry;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.attributes.AttributesStorage;

public class ClientComponentsRegisterImpl implements ClientComponentsRegister {

    private final MySegmentsSynchronizerFactory mMySegmentsSynchronizerFactory;
    private final MySegmentsNotificationProcessorFactory mMySegmentsNotificationProcessorFactory;
    private final SplitStorageContainer mStorageContainer;
    private final AttributesSynchronizerFactory mAttributesSynchronizerFactory;
    private final AttributesSynchronizerRegistry mAttributesSynchronizerRegistry;
    private final MySegmentsSynchronizerRegistry mMySegmentsSynchronizerRegistry;
    private final MySegmentsUpdateWorkerRegistry mMySegmentsUpdateWorkerRegistry;
    private final MySegmentsNotificationProcessorRegistry mMySegmentsNotificationProcessorRegistry;
    private final EventsManagerRegistry mEventsManagerRegistry;
    private final SseAuthenticator mSseAuthenticator;
    private final MySegmentsV2PayloadDecoder mMySegmentsV2PayloadDecoder;
    private final SplitClientConfig mSplitConfig;

    public ClientComponentsRegisterImpl(@NonNull SplitClientConfig splitConfig,
                                        @NonNull MySegmentsSynchronizerFactory mySegmentsSynchronizerFactory,
                                        @NonNull SplitStorageContainer storageContainer,
                                        @NonNull AttributesSynchronizerFactory attributesSynchronizerFactory,
                                        @NonNull AttributesSynchronizerRegistry attributesSynchronizerRegistry,
                                        @NonNull MySegmentsSynchronizerRegistry mySegmentsSynchronizerRegistry,
                                        @Nullable MySegmentsUpdateWorkerRegistry mySegmentsUpdateWorkerRegistry,
                                        @NonNull EventsManagerRegistry eventsManagerRegistry,
                                        @Nullable SseAuthenticator sseAuthenticator,
                                        @Nullable MySegmentsNotificationProcessorRegistry mySegmentsNotificationProcessorRegistry,
                                        @Nullable MySegmentsNotificationProcessorFactory mySegmentsNotificationProcessorFactory,
                                        @Nullable MySegmentsV2PayloadDecoder mySegmentsV2PayloadDecoder) {
        mSplitConfig = splitConfig;
        mMySegmentsSynchronizerFactory = checkNotNull(mySegmentsSynchronizerFactory);
        mStorageContainer = checkNotNull(storageContainer);
        mAttributesSynchronizerFactory = checkNotNull(attributesSynchronizerFactory);
        mAttributesSynchronizerRegistry = checkNotNull(attributesSynchronizerRegistry);
        mEventsManagerRegistry = checkNotNull(eventsManagerRegistry);
        mMySegmentsSynchronizerRegistry = checkNotNull(mySegmentsSynchronizerRegistry);

        // Can be null is singleSyncMode enabled
        mMySegmentsNotificationProcessorRegistry = mySegmentsNotificationProcessorRegistry;
        mMySegmentsUpdateWorkerRegistry = mySegmentsUpdateWorkerRegistry;
        mSseAuthenticator = sseAuthenticator;
        mMySegmentsNotificationProcessorFactory = mySegmentsNotificationProcessorFactory;
        mMySegmentsV2PayloadDecoder = mySegmentsV2PayloadDecoder;
    }

    @Override
    public void registerComponents(Key key, MySegmentsTaskFactory mySegmentsTaskFactory, SplitEventsManager eventsManager) {
        registerEventsManager(key, eventsManager);
        MySegmentsSynchronizer mySegmentsSynchronizer = mMySegmentsSynchronizerFactory.getSynchronizer(mySegmentsTaskFactory, eventsManager);
        registerMySegmentsSynchronizer(key, mySegmentsSynchronizer);
        registerAttributesSynchronizer(key, eventsManager);
        if (isSyncEnabled()) {
            registerKeyInSeeAuthenticator(key);
            LinkedBlockingDeque<MySegmentChangeNotification> mySegmentsNotificationQueue = new LinkedBlockingDeque<>();
            registerMySegmentsNotificationProcessor(key, mySegmentsTaskFactory, mySegmentsNotificationQueue);
            registerMySegmentsUpdateWorker(key, mySegmentsSynchronizer, mySegmentsNotificationQueue);
        }
    }

    @Override
    public void unregisterComponentsForKey(Key key) {
        mAttributesSynchronizerRegistry.unregisterAttributesSynchronizer(key.matchingKey());
        mMySegmentsSynchronizerRegistry.unregisterMySegmentsSynchronizer(key.matchingKey());
        mEventsManagerRegistry.unregisterEventsManager(key);

        if (isSyncEnabled()) {
            mSseAuthenticator.unregisterKey(key.matchingKey());
            mMySegmentsUpdateWorkerRegistry.unregisterMySegmentsUpdateWorker(key.matchingKey());
            mMySegmentsNotificationProcessorRegistry.unregisterMySegmentsProcessor(key.matchingKey());
        }
    }

    private void registerAttributesSynchronizer(Key key, SplitEventsManager eventsManager) {
        AttributesStorage attributesStorage = mStorageContainer.getAttributesStorage(key.matchingKey());
        AttributesSynchronizer attributesSynchronizer = mAttributesSynchronizerFactory.getSynchronizer(
                new AttributeTaskFactoryImpl(key.matchingKey(), attributesStorage), eventsManager
        );

        mAttributesSynchronizerRegistry.registerAttributesSynchronizer(key.matchingKey(),
                attributesSynchronizer);
    }

    private void registerMySegmentsSynchronizer(Key key, MySegmentsSynchronizer mySegmentsSynchronizer) {
        mMySegmentsSynchronizerRegistry.registerMySegmentsSynchronizer(key.matchingKey(),
                mySegmentsSynchronizer);
    }

    private void registerMySegmentsUpdateWorker(Key key, MySegmentsSynchronizer mySegmentsSynchronizer, LinkedBlockingDeque<MySegmentChangeNotification> notificationsQueue) {
        mMySegmentsUpdateWorkerRegistry.registerMySegmentsUpdateWorker(key.matchingKey(),
                new MySegmentsUpdateWorker(mySegmentsSynchronizer, notificationsQueue));
    }

    private void registerEventsManager(Key key, SplitEventsManager eventsManager) {
        mEventsManagerRegistry.registerEventsManager(key, eventsManager);
    }

    private void registerKeyInSeeAuthenticator(Key key) {
        mSseAuthenticator.registerKey(key.matchingKey());
    }

    private void registerMySegmentsNotificationProcessor(Key key, MySegmentsTaskFactory mySegmentsTaskFactory, LinkedBlockingDeque<MySegmentChangeNotification> notificationsQueue) {
        MySegmentsNotificationProcessor processor = getMySegmentsNotificationProcessor(key, mySegmentsTaskFactory, notificationsQueue);
        mMySegmentsNotificationProcessorRegistry.registerMySegmentsProcessor(key.matchingKey(), processor);
    }

    private MySegmentsNotificationProcessor getMySegmentsNotificationProcessor(Key key, MySegmentsTaskFactory mySegmentsTaskFactory, LinkedBlockingDeque<MySegmentChangeNotification> mySegmentUpdateNotificationsQueue) {
        return mMySegmentsNotificationProcessorFactory.getProcessor(
                new MySegmentsNotificationProcessorConfiguration(
                        mySegmentsTaskFactory,
                        mySegmentUpdateNotificationsQueue,
                        mMySegmentsV2PayloadDecoder.hashKey(key.matchingKey())
                )
        );
    }

    private boolean isSyncEnabled() {
        return mSplitConfig.syncEnabled();
    }
}
