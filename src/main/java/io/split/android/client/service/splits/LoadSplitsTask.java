package io.split.android.client.service.splits;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.splits.SplitsStorage;

import static io.split.android.client.utils.Utils.checkNotNull;

/**
 * This task is responsible for loading the feature flags, saved filter & saved flags spec values
 * from the persistent storage into the in-memory storage.
 * It will also update filter and flags spec values if they have changed in the configuration.
 */
public class LoadSplitsTask implements SplitTask {

    private final SplitsStorage mSplitsStorage;
    @NonNull
    private final String mSplitsFilterQueryStringFromConfig;
    @NonNull
    private final String mFlagsSpecFromConfig;

    public LoadSplitsTask(@NonNull SplitsStorage splitsStorage, @Nullable String splitsFilterQueryStringFromConfig, @Nullable String flagsSpecFromConfig) {
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitsFilterQueryStringFromConfig = (splitsFilterQueryStringFromConfig == null) ? "" : splitsFilterQueryStringFromConfig;
        mFlagsSpecFromConfig = (flagsSpecFromConfig == null) ? "" : flagsSpecFromConfig;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        // This call loads the feature flags from the DB into memory, as well as the
        // filter and flags spec values
        mSplitsStorage.loadLocal();

        String queryStringFromStorage = mSplitsStorage.getSplitsFilterQueryString();
        String flagsSpecFromStorage = mSplitsStorage.getFlagsSpec();
        if (queryStringFromStorage == null) {
            queryStringFromStorage = "";
        }

        if (flagsSpecFromStorage == null) {
            flagsSpecFromStorage = "";
        }

        // If change number is not the initial one, and the filter and flags spec have not changed, we don't need to do anything
        boolean isNotInitialChangeNumber = mSplitsStorage.getTill() > -1;
        boolean filterHasNotChanged = mSplitsFilterQueryStringFromConfig.equals(queryStringFromStorage);
        boolean flagsSpecHasNotChanged = mFlagsSpecFromConfig.equals(flagsSpecFromStorage);
        if (isNotInitialChangeNumber && filterHasNotChanged && flagsSpecHasNotChanged) {
            return SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_SPLITS);
        }

        // If the filter or flags spec have changed, we need to clear the cache
        boolean filterHasChanged = !filterHasNotChanged;
        boolean flagsSpecHasChanged = !flagsSpecHasNotChanged;
        boolean shouldClearCache = filterHasChanged || flagsSpecHasChanged;
        if (shouldClearCache) {
            mSplitsStorage.clear();

            if (filterHasChanged) {
                mSplitsStorage.updateSplitsFilterQueryString(mSplitsFilterQueryStringFromConfig);
            }

            if (flagsSpecHasChanged) {
                mSplitsStorage.updateFlagsSpec(mFlagsSpecFromConfig);
            }
        }

        // Since change number is -1 or the storage has been cleared,
        // we don't consider the flags to be loaded, so the task status is error
        return SplitTaskExecutionInfo.error(SplitTaskType.LOAD_LOCAL_SPLITS);
    }
}
