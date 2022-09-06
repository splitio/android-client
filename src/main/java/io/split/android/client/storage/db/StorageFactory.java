package io.split.android.client.storage.db;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.storage.attributes.AttributesStorageContainer;
import io.split.android.client.storage.attributes.AttributesStorageContainerImpl;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.storage.attributes.SqLitePersistentAttributesStorage;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.events.SqLitePersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsCountStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsUniqueStorage;
import io.split.android.client.storage.impressions.SqLitePersistentImpressionsCountStorage;
import io.split.android.client.storage.impressions.SqLitePersistentImpressionsStorage;
import io.split.android.client.storage.impressions.SqlitePersistentUniqueStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainerImpl;
import io.split.android.client.storage.mysegments.SqLitePersistentMySegmentsStorage;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.storage.splits.SplitsStorageImpl;
import io.split.android.client.storage.splits.SqLitePersistentSplitsStorage;
import io.split.android.client.telemetry.storage.BinarySearchLatencyTracker;
import io.split.android.client.telemetry.storage.InMemoryTelemetryStorage;
import io.split.android.client.telemetry.storage.NoOpTelemetryStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;

@RestrictTo(LIBRARY)
public class StorageFactory {

    public static SplitsStorage getSplitsStorage(SplitRoomDatabase splitRoomDatabase) {
        PersistentSplitsStorage persistentSplitsStorage
                = new SqLitePersistentSplitsStorage(splitRoomDatabase);
        return new SplitsStorageImpl(persistentSplitsStorage);
    }

    public static MySegmentsStorageContainer getMySegmentsStorage(SplitRoomDatabase splitRoomDatabase) {
        return getMySegmentsStorageContainer(splitRoomDatabase);
    }

    public static PersistentSplitsStorage getPersistentSplitsStorage(SplitRoomDatabase splitRoomDatabase) {
        return new SqLitePersistentSplitsStorage(splitRoomDatabase);
    }

    public static PersistentImpressionsStorage getPersistentImpressionsStorage(
            SplitRoomDatabase splitRoomDatabase) {
        return new SqLitePersistentImpressionsStorage(splitRoomDatabase,
                ServiceConstants.RECORDED_DATA_EXPIRATION_PERIOD);
    }

    public static PersistentEventsStorage getPersistentEventsStorage(
            SplitRoomDatabase splitRoomDatabase) {
        return new SqLitePersistentEventsStorage(splitRoomDatabase,
                ServiceConstants.RECORDED_DATA_EXPIRATION_PERIOD);
    }

    public static PersistentImpressionsCountStorage getPersistentImpressionsCountStorage(
            SplitRoomDatabase splitRoomDatabase) {
        return new SqLitePersistentImpressionsCountStorage(splitRoomDatabase,
                ServiceConstants.RECORDED_DATA_EXPIRATION_PERIOD);
    }

    public static AttributesStorageContainer getAttributesStorage() {
        return getAttributesStorageContainerInstance();
    }

    public static PersistentAttributesStorage getPersistentSplitsStorage(SplitRoomDatabase splitRoomDatabase, String matchingKey) {
        return new SqLitePersistentAttributesStorage(splitRoomDatabase.attributesDao(), matchingKey);
    }

    public static PersistentImpressionsUniqueStorage getPersistentImpressionsUniqueStorage(SplitRoomDatabase splitRoomDatabase) {
        return new SqlitePersistentUniqueStorage(splitRoomDatabase, ServiceConstants.TEN_DAYS_EXPIRATION_PERIOD);
    }

    // Forces telemetry storage recreation to avoid flaky tests
    @VisibleForTesting
    public static TelemetryStorage getTelemetryStorage(boolean shouldRecordTelemetry) {
        if (shouldRecordTelemetry) {
            return new InMemoryTelemetryStorage(new BinarySearchLatencyTracker());
        }
        return new NoOpTelemetryStorage();
    }

    private static MySegmentsStorageContainer getMySegmentsStorageContainer(SplitRoomDatabase splitRoomDatabase) {
        return new MySegmentsStorageContainerImpl(new SqLitePersistentMySegmentsStorage(splitRoomDatabase));
    }

    private static AttributesStorageContainer getAttributesStorageContainerInstance() {
        return new AttributesStorageContainerImpl();
    }
}
