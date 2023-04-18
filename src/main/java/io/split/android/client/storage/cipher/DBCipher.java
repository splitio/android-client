package io.split.android.client.storage.cipher;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.logger.Logger;

public class DBCipher {

    private final boolean mMustApply;
    private SplitRoomDatabase mSplitDatabase;
    private SplitCipher mFromCipher;
    private SplitCipher mToCipher;

    public DBCipher(@NonNull String apiKey,
                    @NonNull SplitRoomDatabase splitDatabase,
                    @NonNull SplitEncryptionLevel fromLevel,
                    @NonNull SplitEncryptionLevel toLevel,
                    @NonNull SplitCipher toCipher) {
        this(splitDatabase,
                apiKey,
                toCipher,
                fromLevel,
                toLevel);
    }

    private DBCipher(@NonNull SplitRoomDatabase splitDatabase,
                     @NonNull String apiKey,
                     @NonNull SplitCipher toCipher,
                     @NonNull SplitEncryptionLevel fromLevel,
                     @NonNull SplitEncryptionLevel toLevel) {
        mMustApply = fromLevel != toLevel;

        if (mMustApply) {
            Logger.d("Migrating encryption mode");
            mFromCipher = SplitCipherFactory.create(apiKey, fromLevel);
            mToCipher = checkNotNull(toCipher);
            mSplitDatabase = checkNotNull(splitDatabase);
        } else {
            Logger.d("No need to migrate encryption mode");
        }
    }

    @WorkerThread
    public void apply() {
        if (mMustApply) {
            new ApplyCipherTask(mSplitDatabase, mFromCipher,
                    mToCipher).execute();
            Logger.d("Encryption mode migration done");
        }
    }
}
