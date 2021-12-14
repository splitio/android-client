package io.split.android.client.storage.db;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.RestrictTo;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.AttributesStorageImpl;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.storage.attributes.SqLitePersistentAttributesStorage;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.events.SqLitePersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsCountStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.impressions.SqLitePersistentImpressionsCountStorage;
import io.split.android.client.storage.impressions.SqLitePersistentImpressionsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageImpl;
import io.split.android.client.storage.mysegments.PersistentMySegmentsStorage;
import io.split.android.client.storage.mysegments.SqLitePersistentMySegmentsStorage;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.storage.splits.SplitsStorageImpl;
import io.split.android.client.storage.splits.SqLitePersistentSplitsStorage;
import io.split.android.client.telemetry.storage.BinarySearchLatencyTracker;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.telemetry.storage.InMemoryTelemetryStorage;

@RestrictTo(LIBRARY)
public class StorageFactory {

    private static final TelemetryStorage telemetryStorageInstance = new InMemoryTelemetryStorage(new BinarySearchLatencyTracker());

    public static SplitsStorage getSplitsStorage(SplitRoomDatabase splitRoomDatabase) {
        PersistentSplitsStorage persistentSplitsStorage
                = new SqLitePersistentSplitsStorage(splitRoomDatabase);
        return new SplitsStorageImpl(persistentSplitsStorage);
    }

    public static MySegmentsStorage getMySegmentsStorage(SplitRoomDatabase splitRoomDatabase,
                                                         String key) {
        PersistentMySegmentsStorage persistentMySegmentsStorage
                = new SqLitePersistentMySegmentsStorage(splitRoomDatabase, key);
        return new MySegmentsStorageImpl(persistentMySegmentsStorage);
    }

    public static PersistentSplitsStorage getPersistentSplitsStorage(SplitRoomDatabase splitRoomDatabase) {
        return new SqLitePersistentSplitsStorage(splitRoomDatabase);
    }

    public static PersistentImpressionsStorage getPersistenImpressionsStorage(
            SplitRoomDatabase splitRoomDatabase) {
        return new SqLitePersistentImpressionsStorage(splitRoomDatabase,
                ServiceConstants.RECORDED_DATA_EXPIRATION_PERIOD);
    }

    public static PersistentEventsStorage getPersistenEventsStorage(
            SplitRoomDatabase splitRoomDatabase) {
        return new SqLitePersistentEventsStorage(splitRoomDatabase,
                ServiceConstants.RECORDED_DATA_EXPIRATION_PERIOD);
    }

    public static PersistentImpressionsCountStorage getPersistenImpressionsCountStorage(
            SplitRoomDatabase splitRoomDatabase) {
        return new SqLitePersistentImpressionsCountStorage(splitRoomDatabase,
                ServiceConstants.RECORDED_DATA_EXPIRATION_PERIOD);
    }

    public static AttributesStorage getAttributesStorage() {
        return new AttributesStorageImpl();
    }

    public static PersistentAttributesStorage getPersistentSplitsStorage(SplitRoomDatabase splitRoomDatabase, String matchingKey) {
        return new SqLitePersistentAttributesStorage(splitRoomDatabase.attributesDao(), matchingKey);
    }

    public static TelemetryStorage getTelemetryStorage() {
        return telemetryStorageInstance;
    }
}
