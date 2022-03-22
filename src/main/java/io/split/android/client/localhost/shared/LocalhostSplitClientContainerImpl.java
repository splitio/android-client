package io.split.android.client.localhost.shared;

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

    public LocalhostSplitClientContainerImpl(LocalhostSplitFactory splitFactory,
                                             SplitClientConfig config,
                                             SplitsStorage splitsStorage,
                                             SplitParser splitParser,
                                             AttributesManagerFactory attributesManagerFactory,
                                             AttributesMerger attributesMerger,
                                             TelemetryStorageProducer telemetryStorageProducer,
                                             EventsManagerCoordinator eventsManagerCoordinator) {
        mSplitFactory = splitFactory;
        mConfig = config;
        mSplitStorage = splitsStorage;
        mSplitParser = splitParser;
        mAttributesManagerFactory = attributesManagerFactory;
        mAttributesMerger = attributesMerger;
        mTelemetryStorageProducer = telemetryStorageProducer;
        mEventsManagerCoordinator = eventsManagerCoordinator;
    }

    @Override
    protected void createNewClient(Key key) {
        SplitEventsManager eventsManager = new SplitEventsManager(mConfig);
        eventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_FETCHED);
        eventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);

        AttributesStorageImpl attributesStorage = new AttributesStorageImpl();
        AttributesManager attributesManager = mAttributesManagerFactory.getManager(key.matchingKey(), attributesStorage);

        SplitClient client = new LocalhostSplitClient(
                mSplitFactory,
                this,
                mConfig,
                key.matchingKey(),
                mSplitStorage,
                eventsManager,
                mSplitParser,
                attributesManager,
                mAttributesMerger,
                mTelemetryStorageProducer
        );

        eventsManager.getExecutorResources().setSplitClient(client);
        trackNewClient(key.matchingKey(), client);

        mEventsManagerCoordinator.registerEventsManager(key.matchingKey(), eventsManager);
    }
}
