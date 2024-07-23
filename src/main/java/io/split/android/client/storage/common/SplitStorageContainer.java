package io.split.android.client.storage.common;

import androidx.annotation.NonNull;

import io.split.android.client.service.impressions.observer.PersistentImpressionsObserverCacheStorage;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.AttributesStorageContainer;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.storage.events.EventsStorage;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.impressions.ImpressionsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsCountStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsUniqueStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;

import static io.split.android.client.utils.Utils.checkNotNull;

public class SplitStorageContainer {

    private final SplitsStorage mSplitStorage;
    private final MySegmentsStorageContainer mMySegmentsStorageContainer;
    private final MySegmentsStorageContainer mMyLargeSegmentsStorageContainer;
    private final PersistentSplitsStorage mPersistentSplitsStorage;
    private final PersistentEventsStorage mPersistentEventsStorage;
    private final EventsStorage mEventsStorage;
    private final PersistentImpressionsStorage mPersistentImpressionsStorage;
    private final ImpressionsStorage mImpressionsStorage;
    private final PersistentImpressionsCountStorage mPersistentImpressionsCountStorage;
    private final AttributesStorageContainer mAttributesStorageContainer;
    private final PersistentAttributesStorage mPersistentAttributesStorage;
    private final TelemetryStorage mTelemetryStorage;
    private final PersistentImpressionsUniqueStorage mPersistentImpressionsUniqueStorage;
    private final PersistentImpressionsObserverCacheStorage mPersistentImpressionsObserverCacheStorage;

    public SplitStorageContainer(@NonNull SplitsStorage splitStorage,
                                 @NonNull MySegmentsStorageContainer mySegmentsStorageContainer,
                                 @NonNull MySegmentsStorageContainer myLargeSegmentsStorageContainer,
                                 @NonNull PersistentSplitsStorage persistentSplitsStorage,
                                 @NonNull EventsStorage eventsStorage,
                                 @NonNull PersistentEventsStorage persistentEventsStorage,
                                 @NonNull ImpressionsStorage impressionsStorage,
                                 @NonNull PersistentImpressionsStorage persistentImpressionsStorage,
                                 @NonNull PersistentImpressionsCountStorage persistentImpressionsCountStorage,
                                 @NonNull PersistentImpressionsUniqueStorage persistentImpressionsUniqueStorage,
                                 @NonNull AttributesStorageContainer attributesStorageContainer,
                                 @NonNull PersistentAttributesStorage persistentAttributesStorage,
                                 @NonNull TelemetryStorage telemetryStorage,
                                 @NonNull PersistentImpressionsObserverCacheStorage persistentImpressionsObserverCacheStorage) {

        mSplitStorage = checkNotNull(splitStorage);
        mMySegmentsStorageContainer = checkNotNull(mySegmentsStorageContainer);
        mMyLargeSegmentsStorageContainer = checkNotNull(myLargeSegmentsStorageContainer);
        mPersistentSplitsStorage = checkNotNull(persistentSplitsStorage);
        mEventsStorage = checkNotNull(eventsStorage);
        mPersistentEventsStorage = checkNotNull(persistentEventsStorage);
        mImpressionsStorage = checkNotNull(impressionsStorage);
        mPersistentImpressionsStorage = checkNotNull(persistentImpressionsStorage);
        mPersistentImpressionsCountStorage = checkNotNull(persistentImpressionsCountStorage);
        mAttributesStorageContainer = checkNotNull(attributesStorageContainer);
        mPersistentAttributesStorage = checkNotNull(persistentAttributesStorage);
        mTelemetryStorage = checkNotNull(telemetryStorage);
        mPersistentImpressionsUniqueStorage = checkNotNull(persistentImpressionsUniqueStorage);
        mPersistentImpressionsObserverCacheStorage = checkNotNull(persistentImpressionsObserverCacheStorage);
    }

    public SplitsStorage getSplitsStorage() {
        return mSplitStorage;
    }

    public MySegmentsStorageContainer getMySegmentsStorageContainer() {
        return mMySegmentsStorageContainer;
    }

    public MySegmentsStorageContainer getMyLargeSegmentsStorageContainer() {
        return mMyLargeSegmentsStorageContainer;
    }

    public MySegmentsStorage getMySegmentsStorage(String matchingKey) {
        return mMySegmentsStorageContainer.getStorageForKey(matchingKey);
    }

    public MySegmentsStorage getMyLargeSegmentsStorage(String matchingKey) {
        return mMyLargeSegmentsStorageContainer.getStorageForKey(matchingKey);
    }

    public PersistentSplitsStorage getPersistentSplitsStorage() {
        return mPersistentSplitsStorage;
    }

    public EventsStorage getEventsStorage() {
        return mEventsStorage;
    }

    public PersistentEventsStorage getPersistentEventsStorage() {
        return mPersistentEventsStorage;
    }

    public ImpressionsStorage getImpressionsStorage() {
        return mImpressionsStorage;
    }

    public PersistentImpressionsStorage getPersistentImpressionsStorage() {
        return mPersistentImpressionsStorage;
    }

    public PersistentImpressionsCountStorage getImpressionsCountStorage() {
        return mPersistentImpressionsCountStorage;
    }

    public AttributesStorage getAttributesStorage(String matchingKey) {
        return mAttributesStorageContainer.getStorageForKey(matchingKey);
    }

    public AttributesStorageContainer getAttributesStorageContainer() {
        return mAttributesStorageContainer;
    }

    public PersistentAttributesStorage getPersistentAttributesStorage() {
        return mPersistentAttributesStorage;
    }

    public TelemetryStorage getTelemetryStorage() {
        return mTelemetryStorage;
    }

    public PersistentImpressionsUniqueStorage getPersistentImpressionsUniqueStorage() {
        return mPersistentImpressionsUniqueStorage;
    }

    public PersistentImpressionsObserverCacheStorage getImpressionsObserverCachePersistentStorage() {
        return mPersistentImpressionsObserverCacheStorage;
    }
}
