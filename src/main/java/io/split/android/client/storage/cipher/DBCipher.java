package io.split.android.client.storage.cipher;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.logger.Logger;

public class DBCipher {

    private final boolean mMustApply;
    private SplitRoomDatabase mSplitDatabase;
    private SplitCipher mFromCipher;
    private SplitCipher mToCipher;
    private TaskProvider mTaskProvider;

    public DBCipher(@NonNull String apiKey,
                    @NonNull SplitRoomDatabase splitDatabase,
                    @NonNull SplitEncryptionLevel fromLevel,
                    @NonNull SplitEncryptionLevel toLevel,
                    @NonNull SplitCipher toCipher) {
        this(splitDatabase,
                apiKey,
                toCipher,
                fromLevel,
                toLevel,
                new TaskProvider());
    }

    @VisibleForTesting
    public DBCipher(@NonNull SplitRoomDatabase splitDatabase,
                    @NonNull String apiKey,
                    @NonNull SplitCipher toCipher,
                    @NonNull SplitEncryptionLevel fromLevel,
                    @NonNull SplitEncryptionLevel toLevel,
                    @NonNull TaskProvider taskProvider) {
        mMustApply = fromLevel != toLevel;

        if (mMustApply) {
            mFromCipher = SplitCipherFactory.create(apiKey, fromLevel);
            mToCipher = checkNotNull(toCipher);
            mSplitDatabase = checkNotNull(splitDatabase);
            mTaskProvider = checkNotNull(taskProvider);
        }
    }

    @WorkerThread
    public void apply() {
        if (mMustApply) {
            Logger.d("Migrating encryption mode");
            mTaskProvider.get(mSplitDatabase, mFromCipher, mToCipher).execute();
            Logger.d("Encryption mode migration done");
        } else {
            Logger.d("No need to migrate encryption mode");
        }
    }

    static class TaskProvider {
        public ApplyCipherTask get(SplitRoomDatabase splitDatabase,
                                   SplitCipher fromCipher,
                                   SplitCipher toCipher) {
            return new ApplyCipherTask(splitDatabase, fromCipher, toCipher);
        }
    }
}
