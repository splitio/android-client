package io.split.android.client.service.synchronizer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import java.util.concurrent.TimeUnit;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.RolloutDefinitionsCache;
import io.split.android.client.storage.common.SplitStorageContainer;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.utils.logger.Logger;

public class RolloutCacheManagerImpl implements RolloutCacheManager, SplitTask {

    public static final int MIN_CACHE_CLEAR_DAYS = 1;
    @NonNull
    private final GeneralInfoStorage mGeneralInfoStorage;
    @NonNull
    private final SplitTaskExecutor mTaskExecutor;

    @NonNull
    private final RolloutCacheManagerConfig mConfig;

    @NonNull
    private final RolloutDefinitionsCache[] mStorages;

    public RolloutCacheManagerImpl(@NonNull SplitClientConfig splitClientConfig, @NonNull SplitTaskExecutor splitTaskExecutor, @NonNull SplitStorageContainer storageContainer) {
        this(storageContainer.getGeneralInfoStorage(),
                RolloutCacheManagerConfig.from(splitClientConfig),
                splitTaskExecutor,
                storageContainer.getSplitsStorage(),
                storageContainer.getMySegmentsStorageContainer(),
                storageContainer.getMyLargeSegmentsStorageContainer());
    }

    @VisibleForTesting
    RolloutCacheManagerImpl(@NonNull GeneralInfoStorage generalInfoStorage, @NonNull RolloutCacheManagerConfig config, @NonNull SplitTaskExecutor splitTaskExecutor, @NonNull RolloutDefinitionsCache... storages) {
        mGeneralInfoStorage = checkNotNull(generalInfoStorage);
        mStorages = checkNotNull(storages);
        mConfig = checkNotNull(config);
        mTaskExecutor = checkNotNull(splitTaskExecutor);
    }

    @WorkerThread
    @Override
    public void validateCache(SplitTaskExecutionListener listener) {
        mTaskExecutor.submit(this, listener);
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
        // calculate elapsed time since last update
        long lastUpdateTimestamp = mGeneralInfoStorage.getSplitsUpdateTimestamp();
        long daysSinceLastUpdate = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastUpdateTimestamp);

        if (daysSinceLastUpdate > mConfig.getCacheExpirationInDays()) {
            Logger.v("Clearing rollout definitions cache due to expiration");
            return true;
        } else if (mConfig.isClearOnInit()) {
            long lastCacheClearTimestamp = mGeneralInfoStorage.getRolloutCacheLastClearTimestamp();
            long daysSinceCacheClear = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastCacheClearTimestamp);

            // don't clear too soon
            if (daysSinceCacheClear > MIN_CACHE_CLEAR_DAYS) {
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
