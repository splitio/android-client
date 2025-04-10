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
            SplitEncryptionLevel fromLevel = getFromLevel(mSplitDatabase.generalInfoDao(), mEncryptionEnabled);

            // Determine target encryption level
            SplitEncryptionLevel toLevel = getLevel(mEncryptionEnabled);

            DBCipher dbCipher = new DBCipher(mApiKey, mSplitDatabase, fromLevel, toLevel, mToCipher);

            // Apply encryption
            dbCipher.apply();

            // Update encryption level
            updateCurrentLevel(toLevel);

            System.out.println(StartupTimeTracker.getElapsedTimeLog("EncryptionMigrationTask: Completed in " + 
                    (System.currentTimeMillis() - startTime) + "ms"));
            return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        } catch (Exception e) {
            Logger.e("Error while migrating encryption: " + e.getMessage());

            return SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK);
        }
    }

    private void updateCurrentLevel(SplitEncryptionLevel toLevel) {
        mSplitDatabase.generalInfoDao()
                .update(new GeneralInfoEntity(GeneralInfoEntity.DATABASE_ENCRYPTION_MODE,
                        toLevel.toString()));
    }

    @NonNull
    private static SplitEncryptionLevel getFromLevel(GeneralInfoDao generalInfoDao, boolean encryptionEnabled) {
        GeneralInfoEntity entity = generalInfoDao
                .getByName(GeneralInfoEntity.DATABASE_ENCRYPTION_MODE);

        if (entity != null) {
            return SplitEncryptionLevel.fromString(entity.getStringValue());
        }

        return getLevel(encryptionEnabled);
    }

    @NonNull
    private static SplitEncryptionLevel getLevel(boolean encryptionEnabled) {
        return encryptionEnabled ? SplitEncryptionLevel.AES_128_CBC :
                SplitEncryptionLevel.NONE;
    }
}
