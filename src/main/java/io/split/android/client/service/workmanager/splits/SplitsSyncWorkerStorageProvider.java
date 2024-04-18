package io.split.android.client.service.workmanager.splits;

import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class SplitsSyncWorkerStorageProvider implements SplitsSyncWorkerTaskBuilder.StorageProvider {

    private final SplitRoomDatabase mDatabase;
    private final String mApiKey;
    private final boolean mEncryptionEnabled;
    private final boolean mShouldRecordTelemetry;

    SplitsSyncWorkerStorageProvider(SplitRoomDatabase database, String apiKey, boolean encryptionEnabled, boolean shouldRecordTelemetry) {
        mDatabase = database;
        mApiKey = apiKey;
        mEncryptionEnabled = encryptionEnabled;
        mShouldRecordTelemetry = shouldRecordTelemetry;
    }

    @Override
    public SplitsStorage provideSplitsStorage() {
        return StorageFactory.getSplitsStorageForWorker(mDatabase, mApiKey, mEncryptionEnabled);
    }

    @Override
    public TelemetryStorage provideTelemetryStorage() {
        return StorageFactory.getTelemetryStorage(mShouldRecordTelemetry);
    }
}
