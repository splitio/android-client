package io.split.android.client.service.workmanager.splits;

import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageProducer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;

class StorageProvider {

    private final SplitRoomDatabase mDatabase;
    private final boolean mShouldRecordTelemetry;
    private final SplitCipher mCipher;

    StorageProvider(SplitRoomDatabase database, String apiKey, boolean encryptionEnabled, boolean shouldRecordTelemetry) {
        mDatabase = database;
        mCipher = SplitCipherFactory.create(apiKey, encryptionEnabled);
        mShouldRecordTelemetry = shouldRecordTelemetry;
    }

    SplitsStorage provideSplitsStorage() {
        SplitsStorage splitsStorageForWorker = StorageFactory.getSplitsStorage(mDatabase, mCipher);
        splitsStorageForWorker.loadLocal(); // call loadLocal to populate storage with DB data

        return splitsStorageForWorker;
    }

    TelemetryStorage provideTelemetryStorage() {
        return StorageFactory.getTelemetryStorage(mShouldRecordTelemetry);
    }

    RuleBasedSegmentStorageProducer provideRuleBasedSegmentStorage() {
        RuleBasedSegmentStorageProducer ruleBasedSegmentStorageForWorker = StorageFactory.getRuleBasedSegmentStorageForWorker(mDatabase, mCipher);
        ruleBasedSegmentStorageForWorker.loadLocal(); // call loadLocal to populate storage with DB data

        return ruleBasedSegmentStorageForWorker;
    }

    GeneralInfoStorage provideGeneralInfoStorage() {
        return StorageFactory.getGeneralInfoStorage(mDatabase);
    }
}
