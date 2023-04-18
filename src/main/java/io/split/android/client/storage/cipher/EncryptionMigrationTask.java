package io.split.android.client.storage.cipher;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.logger.Logger;

public class EncryptionMigrationTask implements SplitTask {

    private final String mApiKey;
    private final SplitRoomDatabase mSplitDatabase;
    private final AtomicReference<SplitCipher> mSplitCipher;
    private final boolean mEncryptionEnabled;
    private final CountDownLatch mLatch;

    public EncryptionMigrationTask(String apiKey,
                                   SplitRoomDatabase splitDatabase,
                                   AtomicReference<SplitCipher> splitCipher,
                                   CountDownLatch latch,
                                   boolean encryptionEnabled) {
        mApiKey = checkNotNull(apiKey);
        mSplitDatabase = checkNotNull(splitDatabase);
        mSplitCipher = checkNotNull(splitCipher);
        mLatch = checkNotNull(latch);
        mEncryptionEnabled = encryptionEnabled;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            // Get current encryption level
            SplitEncryptionLevel fromLevel = getFromLevel();

            // Set new encryption level
            SplitEncryptionLevel toLevel = getLevel();

            // Apply encryption
            Logger.v("Applying encryption migration from " + fromLevel + " to " + toLevel);
            mSplitCipher.set(new DBCipher(mApiKey, mSplitDatabase,
                    fromLevel, toLevel).apply());

            // Update encryption level
            updateCurrentLevel(toLevel);

            return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        } catch (Exception e) {
            Logger.e("Error while migrating encryption: " + e.getMessage());

            return SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK);
        } finally {
            mLatch.countDown();
        }
    }

    private void updateCurrentLevel(SplitEncryptionLevel toLevel) {
        mSplitDatabase.generalInfoDao()
                .update(new GeneralInfoEntity(GeneralInfoEntity.DATABASE_ENCRYPTION_MODE,
                        toLevel.toString()));
    }

    @NonNull
    private SplitEncryptionLevel getFromLevel() {
        GeneralInfoEntity entity = mSplitDatabase.generalInfoDao()
                .getByName(GeneralInfoEntity.DATABASE_ENCRYPTION_MODE);

        if (entity != null) {
            return SplitEncryptionLevel.fromString(
                    entity.getStringValue());
        }

        return getLevel();
    }

    @NonNull
    private SplitEncryptionLevel getLevel() {
        return mEncryptionEnabled ? SplitEncryptionLevel.AES_128_CBC :
                SplitEncryptionLevel.NONE;
    }
}
