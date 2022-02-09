package io.split.android.client.storage;

import androidx.annotation.NonNull;

import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsCountStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitStorageContainer {

    private final SplitsStorage mSplitStorage;
    private final MySegmentsStorageContainer mMySegmentsStorageContainer;
    private final PersistentSplitsStorage mPersistentSplitsStorage;
    private final PersistentEventsStorage mPersistentEventsStorage;
    private final PersistentImpressionsStorage mPersistentImpressionsStorage;
    private final PersistentImpressionsCountStorage mPersistentImpressionsCountStorage;
    private final AttributesStorage mAttributesStorage;
    private final PersistentAttributesStorage mPersistentAttributesStorage;
    private final TelemetryStorage mTelemetryStorage;

    public SplitStorageContainer(@NonNull SplitsStorage splitStorage,
                                 @NonNull MySegmentsStorageContainer mySegmentsStorageContainer,
                                 @NonNull PersistentSplitsStorage persistentSplitsStorage,
                                 @NonNull PersistentEventsStorage persistentEventsStorage,
                                 @NonNull PersistentImpressionsStorage persistentImpressionsStorage,
                                 @NonNull PersistentImpressionsCountStorage persistentImpressionsCountStorage,
                                 @NonNull AttributesStorage attributesStorage,
                                 @NonNull PersistentAttributesStorage persistentAttributesStorage,
                                 @NonNull TelemetryStorage telemetryStorage) {

        mSplitStorage = checkNotNull(splitStorage);
        mMySegmentsStorageContainer = checkNotNull(mySegmentsStorageContainer);
        mPersistentSplitsStorage = checkNotNull(persistentSplitsStorage);
        mPersistentEventsStorage = checkNotNull(persistentEventsStorage);
        mPersistentImpressionsStorage = checkNotNull(persistentImpressionsStorage);
        mPersistentImpressionsCountStorage = checkNotNull(persistentImpressionsCountStorage);
        mAttributesStorage = checkNotNull(attributesStorage);
        mPersistentAttributesStorage = checkNotNull(persistentAttributesStorage);
        mTelemetryStorage = checkNotNull(telemetryStorage);
    }

    public SplitsStorage getSplitsStorage() {
        return mSplitStorage;
    }

    public MySegmentsStorageContainer getMySegmentsStorageContainer() {
        return mMySegmentsStorageContainer;
    }

    public MySegmentsStorage getMySegmentsStorage(String matchingKey) {
        return mMySegmentsStorageContainer.getStorageForKey(matchingKey);
    }

    public PersistentSplitsStorage getPersistentSplitsStorage() {
        return mPersistentSplitsStorage;
    }

    public PersistentEventsStorage getEventsStorage() {
        return mPersistentEventsStorage;
    }

    public PersistentImpressionsStorage getImpressionsStorage() {
        return mPersistentImpressionsStorage;
    }

    public PersistentImpressionsCountStorage getImpressionsCountStorage() {
        return mPersistentImpressionsCountStorage;
    }

    public AttributesStorage getAttributesStorage() {
        return mAttributesStorage;
    }

    public PersistentAttributesStorage getPersistentAttributesStorage() {
        return mPersistentAttributesStorage;
    }

    public TelemetryStorage getTelemetryStorage() {
        return mTelemetryStorage;
    }
}
