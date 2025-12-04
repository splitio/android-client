package io.split.android.client.localhost.shared;

import androidx.annotation.VisibleForTesting;

import io.split.android.client.FlagSetsFilter;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesManagerFactory;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.EventsManagerCoordinator;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.localhost.LocalhostSplitClient;
import io.split.android.client.localhost.LocalhostSplitFactory;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.shared.BaseSplitClientContainer;
import io.split.android.client.storage.attributes.AttributesStorageImpl;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.engine.experiments.SplitParser;

public class LocalhostSplitClientContainerImpl extends BaseSplitClientContainer {

    private final LocalhostSplitFactory mSplitFactory;
    private final SplitClientConfig mConfig;
    private final SplitsStorage mSplitStorage;
    private final SplitParser mSplitParser;
    private final AttributesManagerFactory mAttributesManagerFactory;
    private final AttributesMerger mAttributesMerger;
    private final TelemetryStorageProducer mTelemetryStorageProducer;
    private final EventsManagerCoordinator mEventsManagerCoordinator;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final FlagSetsFilter mFlagSetsFilter;
    private final SplitEventsManagerFactory mEventsManagerFactory;

    public LocalhostSplitClientContainerImpl(LocalhostSplitFactory splitFactory,
                                             SplitClientConfig config,
                                             SplitsStorage splitsStorage,
                                             SplitParser splitParser,
                                             AttributesManagerFactory attributesManagerFactory,
                                             AttributesMerger attributesMerger,
                                             TelemetryStorageProducer telemetryStorageProducer,
                                             EventsManagerCoordinator eventsManagerCoordinator,
                                             SplitTaskExecutor taskExecutor,
                                             FlagSetsFilter flagSetsFilter) {
        this(splitFactory, config, splitsStorage, splitParser, attributesManagerFactory,
                attributesMerger, telemetryStorageProducer, eventsManagerCoordinator,
                taskExecutor, flagSetsFilter,
                new DefaultSplitEventsManagerFactory(taskExecutor, config));
    }

    @VisibleForTesting
    LocalhostSplitClientContainerImpl(LocalhostSplitFactory splitFactory,
                                      SplitClientConfig config,
                                      SplitsStorage splitsStorage,
                                      SplitParser splitParser,
                                      AttributesManagerFactory attributesManagerFactory,
                                      AttributesMerger attributesMerger,
                                      TelemetryStorageProducer telemetryStorageProducer,
                                      EventsManagerCoordinator eventsManagerCoordinator,
                                      SplitTaskExecutor taskExecutor,
                                      FlagSetsFilter flagSetsFilter,
                                      SplitEventsManagerFactory eventsManagerFactory) {
        mSplitFactory = splitFactory;
        mConfig = config;
        mSplitStorage = splitsStorage;
        mSplitParser = splitParser;
        mAttributesManagerFactory = attributesManagerFactory;
        mAttributesMerger = attributesMerger;
        mTelemetryStorageProducer = telemetryStorageProducer;
        mEventsManagerCoordinator = eventsManagerCoordinator;
        mSplitTaskExecutor = taskExecutor;
        mFlagSetsFilter = flagSetsFilter;
        mEventsManagerFactory = eventsManagerFactory;
    }

    @Override
    protected void createNewClient(Key key) {
        SplitEventsManager eventsManager = mEventsManagerFactory.create();
        eventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventsManager.notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        eventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);

        AttributesStorageImpl attributesStorage = new AttributesStorageImpl();
        AttributesManager attributesManager = mAttributesManagerFactory.getManager(key.matchingKey(), attributesStorage);

        SplitClient client = new LocalhostSplitClient(
                mSplitFactory,
                this,
                mConfig,
                key,
                mSplitStorage,
                eventsManager,
                mSplitParser,
                attributesManager,
                mAttributesMerger,
                mTelemetryStorageProducer,
                mFlagSetsFilter
        );

        eventsManager.getExecutorResources().setSplitClient(client);
        trackNewClient(key, client);

        mEventsManagerCoordinator.registerEventsManager(key, eventsManager);
    }

    @Override
    public void destroy() {
        // No-op
    }

    private static class DefaultSplitEventsManagerFactory implements SplitEventsManagerFactory {
        private final SplitTaskExecutor mTaskExecutor;
        private final int mBlockUntilReady;

        DefaultSplitEventsManagerFactory(SplitTaskExecutor taskExecutor, SplitClientConfig config) {
            mTaskExecutor = taskExecutor;
            mBlockUntilReady = config.blockUntilReady();
        }

        @Override
        public SplitEventsManager create() {
            return new SplitEventsManager(mTaskExecutor, mBlockUntilReady);
        }
    }
}
