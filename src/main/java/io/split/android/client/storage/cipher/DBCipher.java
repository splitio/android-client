package io.split.android.client.storage.cipher;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import io.split.android.client.SplitFactoryImpl.StartupTimeTracker;
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
        System.out.println(StartupTimeTracker.getElapsedTimeLog("DBCipher: Checking if migration is needed"));
        long checkStartTime = System.currentTimeMillis();
        mMustApply = fromLevel != toLevel;
        System.out.println(StartupTimeTracker.getElapsedTimeLog("DBCipher: Migration needed: " + mMustApply + 
                " (from " + fromLevel + " to " + toLevel + "), check took " + 
                (System.currentTimeMillis() - checkStartTime) + "ms"));

        if (mMustApply) {
            System.out.println(StartupTimeTracker.getElapsedTimeLog("DBCipher: Creating cipher for level " + fromLevel));
            long cipherStartTime = System.currentTimeMillis();
            mFromCipher = SplitCipherFactory.create(apiKey, fromLevel);
            System.out.println(StartupTimeTracker.getElapsedTimeLog("DBCipher: Created cipher in " + 
                    (System.currentTimeMillis() - cipherStartTime) + "ms"));
            
            mToCipher = checkNotNull(toCipher);
            mSplitDatabase = checkNotNull(splitDatabase);
            mTaskProvider = checkNotNull(taskProvider);
        }
    }

    @WorkerThread
    public void apply() {
        if (mMustApply) {
            System.out.println(StartupTimeTracker.getElapsedTimeLog("DBCipher: Migrating encryption mode"));
            long taskStartTime = System.currentTimeMillis();
            ApplyCipherTask task = mTaskProvider.get(mSplitDatabase, mFromCipher, mToCipher);
            System.out.println(StartupTimeTracker.getElapsedTimeLog("DBCipher: Created ApplyCipherTask in " + 
                    (System.currentTimeMillis() - taskStartTime) + "ms"));
            
            long executeStartTime = System.currentTimeMillis();
            task.execute();
            System.out.println(StartupTimeTracker.getElapsedTimeLog("DBCipher: Executed ApplyCipherTask in " + 
                    (System.currentTimeMillis() - executeStartTime) + "ms"));
            
            System.out.println(StartupTimeTracker.getElapsedTimeLog("DBCipher: Encryption mode migration done"));
        } else {
            System.out.println(StartupTimeTracker.getElapsedTimeLog("DBCipher: No need to migrate encryption mode"));
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
