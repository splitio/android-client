package io.split.android.client.storage.cipher;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

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
    private final AtomicReference<SplitCipher> mSplitCipher;
    private final boolean mEncryptionEnabled;
    private final CountDownLatch mCountDownLatch;

    public EncryptionMigrationTask(String apiKey,
                                   SplitRoomDatabase splitDatabase,
                                   AtomicReference<SplitCipher> splitCipher,
                                   CountDownLatch countDownLatch,
                                   boolean encryptionEnabled) {
        mApiKey = checkNotNull(apiKey);
        mSplitDatabase = checkNotNull(splitDatabase);
        mSplitCipher = checkNotNull(splitCipher);
        mCountDownLatch = checkNotNull(countDownLatch);
        mEncryptionEnabled = encryptionEnabled;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            // Get current encryption level
            SplitEncryptionLevel fromLevel = getFromLevel(mSplitDatabase.generalInfoDao(), mEncryptionEnabled);

            // Determine target encryption level
            SplitEncryptionLevel toLevel = getLevel(mEncryptionEnabled);

            // Create target level cipher
            SplitCipher toCipher = SplitCipherFactory.create(mApiKey, toLevel);

            // Return cipher to be used by the SDK
            mSplitCipher.set(toCipher);
            mCountDownLatch.countDown();

            // Apply encryption
            new DBCipher(mApiKey, mSplitDatabase, fromLevel, toLevel, toCipher).apply();

            // Update encryption level
            updateCurrentLevel(toLevel);

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
            return SplitEncryptionLevel.fromString(
                    entity.getStringValue());
        }

        return getLevel(encryptionEnabled);
    }

    @NonNull
    private static SplitEncryptionLevel getLevel(boolean encryptionEnabled) {
        return encryptionEnabled ? SplitEncryptionLevel.AES_128_CBC :
                SplitEncryptionLevel.NONE;
    }
}
