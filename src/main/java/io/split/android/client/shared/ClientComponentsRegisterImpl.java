package io.split.android.client.shared;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.LinkedBlockingDeque;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.api.Key;
import io.split.android.client.events.EventsManagerRegistry;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.attributes.AttributeTaskFactoryImpl;
import io.split.android.client.service.mysegments.MySegmentUpdateParams;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.memberships.MembershipsNotificationProcessor;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessorConfiguration;
import io.split.android.client.service.sseclient.notifications.mysegments.MembershipsNotificationProcessorFactory;
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
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.common.SplitStorageContainer;

public class ClientComponentsRegisterImpl implements ClientComponentsRegister {

    private final MySegmentsSynchronizerFactory mMySegmentsSynchronizerFactory;
    private final MembershipsNotificationProcessorFactory mMembershipsNotificationProcessorFactory;
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
                                        @Nullable MembershipsNotificationProcessorFactory membershipsNotificationProcessorFactory,
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
        mMembershipsNotificationProcessorFactory = membershipsNotificationProcessorFactory;
        mMySegmentsV2PayloadDecoder = mySegmentsV2PayloadDecoder;
    }

    @Override
    public void registerComponents(Key key, SplitEventsManager eventsManager, MySegmentsTaskFactory mySegmentsTaskFactory) {
        registerEventsManager(key, eventsManager);

        MySegmentsSynchronizer mySegmentsSynchronizer = mMySegmentsSynchronizerFactory.getSynchronizer(mySegmentsTaskFactory, eventsManager, SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE, mSplitConfig.segmentsRefreshRate());
        registerMySegmentsSynchronizer(key, mySegmentsSynchronizer);

        registerAttributesSynchronizer(key, eventsManager);

        if (isSyncEnabled()) {
            registerKeyInSeeAuthenticator(key);
            LinkedBlockingDeque<MySegmentUpdateParams> mySegmentsNotificationQueue = new LinkedBlockingDeque<>();
            registerMembershipsNotificationProcessor(key, mySegmentsTaskFactory, mySegmentsNotificationQueue);
            registerMySegmentsUpdateWorker(key, mySegmentsSynchronizer, mySegmentsNotificationQueue);

            registerMyLargeSegmentsUpdateWorker(key, mySegmentsSynchronizer, mySegmentsNotificationQueue);
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

    private void registerMySegmentsUpdateWorker(Key key, MySegmentsSynchronizer mySegmentsSynchronizer, LinkedBlockingDeque<MySegmentUpdateParams> notificationsQueue) {
        mMySegmentsUpdateWorkerRegistry.registerMySegmentsUpdateWorker(key.matchingKey(),
                new MySegmentsUpdateWorker(mySegmentsSynchronizer, notificationsQueue));
    }

    private void registerMyLargeSegmentsUpdateWorker(Key key, MySegmentsSynchronizer mySegmentsSynchronizer, LinkedBlockingDeque<MySegmentUpdateParams> notificationsQueue) {
        mMySegmentsUpdateWorkerRegistry.registerMyLargeSegmentsUpdateWorker(key.matchingKey(),
                new MySegmentsUpdateWorker(mySegmentsSynchronizer, notificationsQueue));
    }

    private void registerEventsManager(Key key, SplitEventsManager eventsManager) {
        mEventsManagerRegistry.registerEventsManager(key, eventsManager);
    }

    private void registerKeyInSeeAuthenticator(Key key) {
        mSseAuthenticator.registerKey(key.matchingKey());
    }

    private void registerMembershipsNotificationProcessor(Key key, MySegmentsTaskFactory mySegmentsTaskFactory, LinkedBlockingDeque<MySegmentUpdateParams> notificationsQueue) {
        MembershipsNotificationProcessor processor = getMembershipsNotificationProcessor(key, mySegmentsTaskFactory, notificationsQueue);
        mMySegmentsNotificationProcessorRegistry.registerMySegmentsProcessor(key.matchingKey(), processor);
    }

    private MembershipsNotificationProcessor getMembershipsNotificationProcessor(Key key, MySegmentsTaskFactory mySegmentsTaskFactory, LinkedBlockingDeque<MySegmentUpdateParams> mySegmentUpdateNotificationsQueue) {
        return mMembershipsNotificationProcessorFactory.getProcessor(
                new MySegmentsNotificationProcessorConfiguration(
                        mySegmentsTaskFactory,
                        mySegmentUpdateNotificationsQueue,
                        key.matchingKey(),
                        mMySegmentsV2PayloadDecoder.hashKey(key.matchingKey())));
    }

    private boolean isSyncEnabled() {
        return mSplitConfig.syncEnabled();
    }
}
