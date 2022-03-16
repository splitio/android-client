package io.split.android.client.localhost.shared;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.storage.attributes.AttributesStorageImpl;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.engine.experiments.SplitParser;

public class LocalhostSplitClientContainerImpl implements SplitClientContainer  {

    private final LocalhostSplitFactory mSplitFactory;
    private final SplitClientConfig mConfig;
    private final ConcurrentMap<String, SplitClient> mClientInstances = new ConcurrentHashMap<>();
    private final Object mClientCreationLock = new Object();
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
    public SplitClient getClient(Key key) {
        return getOrCreateClientForKey(key);
    }

    @Override
    public void remove(String key) {
        mClientInstances.remove(key);
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
        eventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_FETCHED);
        eventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);

        AttributesStorageImpl attributesStorage = new AttributesStorageImpl();
        AttributesManager attributesManager = mAttributesManagerFactory.getManager(key.matchingKey(), attributesStorage);

        SplitClient client = new LocalhostSplitClient(
                mSplitFactory,
                mConfig,
                key.matchingKey(),
                mSplitStorage,
                eventsManager,
                mSplitParser,
                attributesManager,
                mAttributesMerger,
                mTelemetryStorageProducer
        );

        mClientInstances.put(key.matchingKey(), client);

        mEventsManagerCoordinator.registerEventsManager(key.matchingKey(), eventsManager);
    }
}
