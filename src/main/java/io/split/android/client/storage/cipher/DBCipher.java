package io.split.android.client.storage.cipher;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.logger.Logger;

public class DBCipher {

    private final SplitRoomDatabase mSplitDatabase;
    private final SplitCipher mFromCipher;
    private final SplitCipher mToCipher;
    private final boolean mMustApply;

    public DBCipher(@NonNull String apiKey,
                    @NonNull SplitRoomDatabase splitDatabase,
                    @NonNull SplitEncryptionLevel fromLevel,
                    @NonNull SplitEncryptionLevel toLevel) {
        this(splitDatabase,
                SplitCipherFactory.create(apiKey, fromLevel),
                SplitCipherFactory.create(apiKey, toLevel));
    }

    private DBCipher(@NonNull SplitRoomDatabase splitDatabase,
                     @NonNull SplitCipher fromCipher,
                    @NonNull SplitCipher toCipher) {
        mSplitDatabase = checkNotNull(splitDatabase);
        mFromCipher = checkNotNull(fromCipher);
        mToCipher = checkNotNull(toCipher);
        mMustApply = !mFromCipher.getClass().equals(mToCipher.getClass());

        if (mMustApply) {
            Logger.v("Migrating encryption mode");
        } else {
            Logger.v("No need to migrate encryption mode");
        }
    }

    @WorkerThread
    public SplitCipher apply() {
        if (mMustApply) {
            new ApplyCipherTask(mSplitDatabase, mFromCipher,
                    mToCipher).execute();
        }

        Logger.v("Encryption mode migration done");
        return mToCipher;
    }
}
