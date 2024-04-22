package io.split.android.client.service.workmanager.splits;

import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;

class StorageProvider {

    private final SplitRoomDatabase mDatabase;
    private final String mApiKey;
    private final boolean mEncryptionEnabled;
    private final boolean mShouldRecordTelemetry;

    StorageProvider(SplitRoomDatabase database, String apiKey, boolean encryptionEnabled, boolean shouldRecordTelemetry) {
        mDatabase = database;
        mApiKey = apiKey;
        mEncryptionEnabled = encryptionEnabled;
        mShouldRecordTelemetry = shouldRecordTelemetry;
    }

    SplitsStorage provideSplitsStorage() {
        SplitsStorage splitsStorageForWorker = StorageFactory.getSplitsStorageForWorker(mDatabase, mApiKey, mEncryptionEnabled);
        splitsStorageForWorker.loadLocal(); // call loadLocal to populate storage with DB data

        return splitsStorageForWorker;
    }

    TelemetryStorage provideTelemetryStorage() {
        return StorageFactory.getTelemetryStorage(mShouldRecordTelemetry);
    }
}
