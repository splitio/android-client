package io.split.android.client.storage.rbs;

import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.general.GeneralInfoStorage;

public class SqLitePersistentRuleBasedSegmentStorageProvider implements PersistentRuleBasedSegmentStorage.Provider {

    private final SqLitePersistentRuleBasedSegmentStorage mPersistentStorage;

    public SqLitePersistentRuleBasedSegmentStorageProvider(SplitCipher cipher, SplitRoomDatabase database, GeneralInfoStorage generalInfoStorage) {
        mPersistentStorage = new SqLitePersistentRuleBasedSegmentStorage(cipher, database, generalInfoStorage);
    }

    @Override
    public PersistentRuleBasedSegmentStorage get() {
        return mPersistentStorage;
    }
}
