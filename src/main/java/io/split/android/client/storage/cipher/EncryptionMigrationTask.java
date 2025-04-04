package io.split.android.client.storage.cipher;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.SplitFactoryImpl.StartupTimeTracker;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.db.GeneralInfoDao;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.logger.Logger;

public class EncryptionMigrationTask implements SplitTask {

    private final String mApiKey;
    private final SplitRoomDatabase mSplitDatabase;
    private final boolean mEncryptionEnabled;
    private final SplitCipher mToCipher;

    public EncryptionMigrationTask(String apiKey,
                                   SplitRoomDatabase splitDatabase,
                                   boolean encryptionEnabled,
                                   SplitCipher toCipher) {
        mApiKey = checkNotNull(apiKey);
        mSplitDatabase = checkNotNull(splitDatabase);
        mEncryptionEnabled = encryptionEnabled;
        mToCipher = checkNotNull(toCipher);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask: Starting execution"));
            long startTime = System.currentTimeMillis();
            
            // Get current encryption level
            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask: Getting current encryption level"));
            long fromLevelStartTime = System.currentTimeMillis();
            SplitEncryptionLevel fromLevel = getFromLevel(mSplitDatabase.generalInfoDao(), mEncryptionEnabled);
            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask: Got current encryption level in " + 
                    (System.currentTimeMillis() - fromLevelStartTime) + "ms: " + fromLevel));

            // Determine target encryption level
            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask: Getting target encryption level"));
            long toLevelStartTime = System.currentTimeMillis();
            SplitEncryptionLevel toLevel = getLevel(mEncryptionEnabled);
            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask: Got target encryption level in " + 
                    (System.currentTimeMillis() - toLevelStartTime) + "ms: " + toLevel));
            
            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask: Creating DBCipher"));
            long dbCipherStartTime = System.currentTimeMillis();
            DBCipher dbCipher = new DBCipher(mApiKey, mSplitDatabase, fromLevel, toLevel, mToCipher);
            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask: Created DBCipher in " + 
                    (System.currentTimeMillis() - dbCipherStartTime) + "ms"));

            // Apply encryption
            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask: Applying encryption"));
            long applyStartTime = System.currentTimeMillis();
            dbCipher.apply();
            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask: Applied encryption in " + 
                    (System.currentTimeMillis() - applyStartTime) + "ms"));

            // Update encryption level
            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask: Updating encryption level"));
            long updateLevelStartTime = System.currentTimeMillis();
            updateCurrentLevel(toLevel);
            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask: Updated encryption level in " + 
                    (System.currentTimeMillis() - updateLevelStartTime) + "ms"));

            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask: Completed in " + 
                    (System.currentTimeMillis() - startTime) + "ms"));
            return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        } catch (Exception e) {
            Logger.e("Error while migrating encryption: " + e.getMessage());

            return SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK);
        }
    }

    private void updateCurrentLevel(SplitEncryptionLevel toLevel) {
        System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask.updateCurrentLevel: Updating encryption level in database to " + toLevel));
        long dbStartTime = System.currentTimeMillis();
        mSplitDatabase.generalInfoDao()
                .update(new GeneralInfoEntity(GeneralInfoEntity.DATABASE_ENCRYPTION_MODE,
                        toLevel.toString()));
        System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask.updateCurrentLevel: Database update took " + 
                (System.currentTimeMillis() - dbStartTime) + "ms"));
    }

    @NonNull
    private static SplitEncryptionLevel getFromLevel(GeneralInfoDao generalInfoDao, boolean encryptionEnabled) {
        System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask.getFromLevel: Getting encryption mode from database"));
        long dbStartTime = System.currentTimeMillis();
        GeneralInfoEntity entity = generalInfoDao
                .getByName(GeneralInfoEntity.DATABASE_ENCRYPTION_MODE);
        System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask.getFromLevel: Database query took " + 
                (System.currentTimeMillis() - dbStartTime) + "ms"));

        if (entity != null) {
            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask.getFromLevel: Found existing encryption mode: " + entity.getStringValue()));
            return SplitEncryptionLevel.fromString(
                    entity.getStringValue());
        }

        System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask.getFromLevel: No existing encryption mode found, using default"));
        return getLevel(encryptionEnabled);
    }

    @NonNull
    private static SplitEncryptionLevel getLevel(boolean encryptionEnabled) {
        return encryptionEnabled ? SplitEncryptionLevel.AES_128_CBC :
                SplitEncryptionLevel.NONE;
    }
}
