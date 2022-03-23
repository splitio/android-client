package io.split.android.client.shared;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.LinkedBlockingDeque;

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
    private final String mDefaultMatchingKey;
    private final MySegmentsV2PayloadDecoder mMySegmentsV2PayloadDecoder;

    public ClientComponentsRegisterImpl(@NonNull MySegmentsSynchronizerFactory mySegmentsSynchronizerFactory,
                                        @NonNull SplitStorageContainer storageContainer,
                                        @NonNull AttributesSynchronizerFactory attributesSynchronizerFactory,
                                        @NonNull AttributesSynchronizerRegistry attributesSynchronizerRegistry,
                                        @NonNull MySegmentsSynchronizerRegistry mySegmentsSynchronizerRegistry,
                                        @NonNull MySegmentsUpdateWorkerRegistry mySegmentsUpdateWorkerRegistry,
                                        @NonNull EventsManagerRegistry eventsManagerRegistry,
                                        @NonNull SseAuthenticator sseAuthenticator,
                                        @NonNull MySegmentsNotificationProcessorRegistry mySegmentsNotificationProcessorRegistry,
                                        @NonNull String defaultMatchingKey,
                                        @NonNull MySegmentsNotificationProcessorFactory mySegmentsNotificationProcessorFactory,
                                        @NonNull MySegmentsV2PayloadDecoder mySegmentsV2PayloadDecoder) {
        mMySegmentsSynchronizerFactory = checkNotNull(mySegmentsSynchronizerFactory);
        mStorageContainer = checkNotNull(storageContainer);
        mAttributesSynchronizerFactory = checkNotNull(attributesSynchronizerFactory);
        mAttributesSynchronizerRegistry = checkNotNull(attributesSynchronizerRegistry);
        mMySegmentsSynchronizerRegistry = checkNotNull(mySegmentsSynchronizerRegistry);
        mMySegmentsUpdateWorkerRegistry = checkNotNull(mySegmentsUpdateWorkerRegistry);
        mMySegmentsNotificationProcessorRegistry = checkNotNull(mySegmentsNotificationProcessorRegistry);
        mEventsManagerRegistry = checkNotNull(eventsManagerRegistry);
        mSseAuthenticator = checkNotNull(sseAuthenticator);
        mDefaultMatchingKey = checkNotNull(defaultMatchingKey);
        mMySegmentsNotificationProcessorFactory = checkNotNull(mySegmentsNotificationProcessorFactory);
        mMySegmentsV2PayloadDecoder = checkNotNull(mySegmentsV2PayloadDecoder);
    }

    @Override
    public void registerComponents(Key key, MySegmentsTaskFactory mySegmentsTaskFactory, SplitEventsManager eventsManager) {
        registerKeyInSeeAuthenticator(key);
        LinkedBlockingDeque<MySegmentChangeNotification> mySegmentsNotificationQueue = new LinkedBlockingDeque<>();
        registerMySegmentsNotificationProcessor(key, mySegmentsTaskFactory, mySegmentsNotificationQueue);
        registerEventsManager(key, eventsManager);
        registerMySegmentsSynchronization(key, mySegmentsTaskFactory, eventsManager, mySegmentsNotificationQueue);
        registerAttributesSynchronizer(key, eventsManager);
    }

    @Override
    public void unregisterComponentsForKey(String key) {
        mSseAuthenticator.unregisterKey(key);
        mAttributesSynchronizerRegistry.unregisterAttributesSynchronizer(key);
        mMySegmentsSynchronizerRegistry.unregisterMySegmentsSynchronizer(key);
        mMySegmentsUpdateWorkerRegistry.unregisterMySegmentsUpdateWorker(key);
        mMySegmentsNotificationProcessorRegistry.unregisterMySegmentsProcessor(key);
    }

    private void registerMySegmentsSynchronization(Key key, MySegmentsTaskFactory mySegmentsTaskFactory, SplitEventsManager eventsManager, LinkedBlockingDeque<MySegmentChangeNotification> notificationsQueue) {
        MySegmentsSynchronizer mySegmentsSynchronizer = mMySegmentsSynchronizerFactory.getSynchronizer(mySegmentsTaskFactory, eventsManager);
        registerMySegmentsUpdateWorker(key, mySegmentsSynchronizer, notificationsQueue);
        registerMySegmentsSynchronizer(key, mySegmentsSynchronizer);
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
        mEventsManagerRegistry.registerEventsManager(key.matchingKey(), eventsManager);
    }

    private void registerKeyInSeeAuthenticator(Key key) {
        mSseAuthenticator.registerKey(key.matchingKey());
    }

    private void registerMySegmentsNotificationProcessor(Key key, MySegmentsTaskFactory mySegmentsTaskFactory, LinkedBlockingDeque<MySegmentChangeNotification> notificationsQueue) {
        MySegmentsNotificationProcessor processor = getMySegmentsNotificationProcessor(key, mySegmentsTaskFactory, notificationsQueue);
        mMySegmentsNotificationProcessorRegistry.registerMySegmentsProcessor(mDefaultMatchingKey, processor);
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
}
