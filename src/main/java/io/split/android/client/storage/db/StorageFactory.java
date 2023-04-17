package io.split.android.client.storage.db;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.storage.attributes.AttributesStorageContainer;
import io.split.android.client.storage.attributes.AttributesStorageContainerImpl;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.storage.attributes.SqLitePersistentAttributesStorage;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.events.EventsStorage;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.events.SqLitePersistentEventsStorage;
import io.split.android.client.storage.impressions.ImpressionsStorage;
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
import io.split.android.client.telemetry.storage.InMemoryTelemetryStorage;
import io.split.android.client.telemetry.storage.NoOpTelemetryStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;

@RestrictTo(LIBRARY)
public class StorageFactory {

    public static SplitsStorage getSplitsStorage(SplitRoomDatabase splitRoomDatabase, SplitCipher splitCipher) {
        PersistentSplitsStorage persistentSplitsStorage
                = getPersistentSplitsStorage(splitRoomDatabase, splitCipher);
        return new SplitsStorageImpl(persistentSplitsStorage);
    }

    public static SplitsStorage getSplitsStorageForWorker(SplitRoomDatabase splitRoomDatabase, String apiKey, boolean encryptionEnabled) {
        return getSplitsStorage(splitRoomDatabase, SplitCipherFactory.create(apiKey, encryptionEnabled));
    }

    public static MySegmentsStorageContainer getMySegmentsStorage(SplitRoomDatabase splitRoomDatabase, SplitCipher splitCipher) {
        return getMySegmentsStorageContainer(splitRoomDatabase, splitCipher);
    }

    public static MySegmentsStorageContainer getMySegmentsStorageForWorker(SplitRoomDatabase splitRoomDatabase, String apiKey, boolean encryptionEnabled) {
        return getMySegmentsStorageContainer(splitRoomDatabase, SplitCipherFactory.create(apiKey, encryptionEnabled));
    }

    public static EventsStorage getEventsStorage(PersistentEventsStorage persistentEventsStorage,
                                                      boolean isPersistenceEnabled) {
        return new EventsStorage(persistentEventsStorage, isPersistenceEnabled);
    }
    public static PersistentSplitsStorage getPersistentSplitsStorage(SplitRoomDatabase splitRoomDatabase, SplitCipher splitCipher) {
        return new SqLitePersistentSplitsStorage(splitRoomDatabase, splitCipher);
    }

    public static ImpressionsStorage getImpressionsStorage(PersistentImpressionsStorage persistentImpressionsStorage,
                                                           boolean isPersistenceEnabled) {
        return new ImpressionsStorage(persistentImpressionsStorage, isPersistenceEnabled);
    }

    public static PersistentImpressionsStorage getPersistentImpressionsStorage(
            SplitRoomDatabase splitRoomDatabase, SplitCipher splitCipher) {
        return new SqLitePersistentImpressionsStorage(splitRoomDatabase,
                ServiceConstants.RECORDED_DATA_EXPIRATION_PERIOD, splitCipher);
    }

    public static PersistentImpressionsStorage getPersistentImpressionsStorageForWorker(
            SplitRoomDatabase splitRoomDatabase, String apiKey, boolean encryptionEnabled) {
        return getPersistentImpressionsStorage(splitRoomDatabase, SplitCipherFactory.create(apiKey, encryptionEnabled));
    }

    public static PersistentEventsStorage getPersistentEventsStorage(
            SplitRoomDatabase splitRoomDatabase, SplitCipher splitCipher) {
        return new SqLitePersistentEventsStorage(splitRoomDatabase,
                ServiceConstants.RECORDED_DATA_EXPIRATION_PERIOD, splitCipher);
    }

    public static PersistentEventsStorage getPersistentEventsStorageForWorker(
            SplitRoomDatabase splitRoomDatabase, String apiKey, boolean encryptionEnabled) {
        return getPersistentEventsStorage(splitRoomDatabase, SplitCipherFactory.create(apiKey, encryptionEnabled));
    }

    public static PersistentImpressionsCountStorage getPersistentImpressionsCountStorage(
            SplitRoomDatabase splitRoomDatabase) {
        return new SqLitePersistentImpressionsCountStorage(splitRoomDatabase,
                ServiceConstants.RECORDED_DATA_EXPIRATION_PERIOD);
    }

    public static AttributesStorageContainer getAttributesStorage() {
        return getAttributesStorageContainerInstance();
    }

    public static PersistentAttributesStorage getPersistentAttributesStorage(SplitRoomDatabase splitRoomDatabase, String matchingKey) {
        return new SqLitePersistentAttributesStorage(splitRoomDatabase.attributesDao(), matchingKey);
    }

    public static PersistentImpressionsUniqueStorage getPersistentImpressionsUniqueStorage(SplitRoomDatabase splitRoomDatabase) {
        return new SqlitePersistentUniqueStorage(splitRoomDatabase, ServiceConstants.TEN_DAYS_EXPIRATION_PERIOD);
    }

    // Forces telemetry storage recreation to avoid flaky tests
    public static TelemetryStorage getTelemetryStorage(boolean shouldRecordTelemetry) {
        if (shouldRecordTelemetry) {
            return new InMemoryTelemetryStorage();
        }
        return new NoOpTelemetryStorage();
    }

    private static MySegmentsStorageContainer getMySegmentsStorageContainer(SplitRoomDatabase splitRoomDatabase, SplitCipher splitCipher) {
        return new MySegmentsStorageContainerImpl(new SqLitePersistentMySegmentsStorage(splitRoomDatabase, splitCipher));
    }

    private static AttributesStorageContainer getAttributesStorageContainerInstance() {
        return new AttributesStorageContainerImpl();
    }
}
