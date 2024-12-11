package io.split.android.client.service.synchronizer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import java.util.concurrent.TimeUnit;

import io.split.android.client.RolloutCacheConfiguration;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.CleanUpDatabaseTask;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.RolloutDefinitionsCache;
import io.split.android.client.storage.cipher.EncryptionMigrationTask;
import io.split.android.client.storage.common.SplitStorageContainer;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.utils.logger.Logger;

public class RolloutCacheManagerImpl implements RolloutCacheManager, SplitTask {

    public static final int MIN_CACHE_CLEAR_DAYS = 1; // TODO

    @NonNull
    private final GeneralInfoStorage mGeneralInfoStorage;
    @NonNull
    private final RolloutCacheConfiguration mConfig;
    @NonNull
    private final RolloutDefinitionsCache[] mStorages;
    @NonNull
    private final CleanUpDatabaseTask mCleanUpDatabaseTask;
    @NonNull
    private final EncryptionMigrationTask mEncryptionMigrationTask;

    public RolloutCacheManagerImpl(@NonNull SplitClientConfig splitClientConfig,
                                   @NonNull SplitStorageContainer storageContainer,
                                   @NonNull CleanUpDatabaseTask cleanUpDatabaseTask,
                                   @NonNull EncryptionMigrationTask encryptionMigrationTask) {
        this(storageContainer.getGeneralInfoStorage(),
                splitClientConfig.rolloutCacheConfiguration(),
                cleanUpDatabaseTask,
                encryptionMigrationTask,
                storageContainer.getSplitsStorage(),
                storageContainer.getMySegmentsStorageContainer(),
                storageContainer.getMyLargeSegmentsStorageContainer());
    }

    @VisibleForTesting
    RolloutCacheManagerImpl(@NonNull GeneralInfoStorage generalInfoStorage,
                            @NonNull RolloutCacheConfiguration config,
                            @NonNull CleanUpDatabaseTask cleanUpDatabaseTask,
                            @NonNull EncryptionMigrationTask encryptionMigrationTask,
                            @NonNull RolloutDefinitionsCache... storages) {
        mGeneralInfoStorage = checkNotNull(generalInfoStorage);
        mCleanUpDatabaseTask = checkNotNull(cleanUpDatabaseTask);
        mEncryptionMigrationTask = checkNotNull(encryptionMigrationTask);
        mStorages = checkNotNull(storages);
        mConfig = checkNotNull(config);
    }

    @WorkerThread
    @Override
    public void validateCache(SplitTaskExecutionListener listener) {
        try {
            Logger.v("Rollout cache manager: Executing clearing task");
            mCleanUpDatabaseTask.execute();
            Logger.v("Rollout cache manager: Validating cache");
            execute();
            Logger.v("Rollout cache manager: Migrating encryption");
            mEncryptionMigrationTask.execute();
            Logger.v("Rollout cache manager: Validation finished");
            listener.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));
        } catch (Exception ex) {
            Logger.e("Error occurred validating cache: " + ex.getMessage());
            listener.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK));
        }
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            boolean expired = validateExpiration();
            if (expired) {
                clear();
            }
        } catch (Exception e) {
            Logger.e("Error occurred validating cache: " + e.getMessage());

            return SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK);
        }
        return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
    }

    private boolean validateExpiration() {
        long lastUpdateTimestamp = mGeneralInfoStorage.getSplitsUpdateTimestamp();
        // calculate elapsed time since last update
        long daysSinceLastUpdate = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastUpdateTimestamp);

        if (lastUpdateTimestamp > 0 && daysSinceLastUpdate >= mConfig.getExpirationDays()) {
            Logger.v("Clearing rollout definitions cache due to expiration");
            return true;
        } else if (mConfig.isClearOnInit()) {
            long lastCacheClearTimestamp = mGeneralInfoStorage.getRolloutCacheLastClearTimestamp();
            if (lastCacheClearTimestamp < 1) { // 0 is default value for rollout cache timestamp
                return true;
            }
            long daysSinceCacheClear = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastCacheClearTimestamp);

            // don't clear too soon
            if (daysSinceCacheClear >= MIN_CACHE_CLEAR_DAYS) {
                Logger.v("Forcing rollout definitions cache clear");
                return true;
            }
        }

        return false;
    }

    private void clear() {
        for (RolloutDefinitionsCache storage : mStorages) {
            storage.clear();
        }
        mGeneralInfoStorage.setRolloutCacheLastClearTimestamp(System.currentTimeMillis());
        Logger.v("Rollout definitions cache cleared");
    }
}
