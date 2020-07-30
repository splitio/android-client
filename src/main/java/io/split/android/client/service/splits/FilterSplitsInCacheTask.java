package io.split.android.client.service.splits;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.client.SplitFilter;
import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class FilterSplitsInCacheTask implements SplitTask {

    private final static String PREFIX_SEPARATOR = "__";
    private final PersistentSplitsStorage mSplitsStorage;
    private final List<SplitFilter> mSplitsFilter;
    private final String mSplitsFilterQueryString;

    public FilterSplitsInCacheTask(@NonNull PersistentSplitsStorage splitsStorage,
                                   @NonNull List<SplitFilter> splitsFilter,
                                   @Nullable String splitsFilterQueryString) {
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitsFilter = checkNotNull(splitsFilter);
        mSplitsFilterQueryString = splitsFilterQueryString;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {

        if (!queryStringHasChanged()) {
            return SplitTaskExecutionInfo.success(SplitTaskType.FILTER_SPLITS_CACHE);
        }

        Set<String> namesToKeep = new HashSet<>();
        Set<String> prefixesToKeep = new HashSet<>();
        for (SplitFilter filter : mSplitsFilter) {
            switch (filter.getType()) {
                case BY_NAME:
                    namesToKeep.addAll(filter.getValues());
                    break;
                case BY_PREFIX:
                    prefixesToKeep.addAll(filter.getValues());
                    break;
                default:
                    Logger.e("Unknown filter type" + filter.getType().toString());
            }
        }

        List<String> splitsToDelete = new ArrayList<>();
        List<Split> splitsInCache = mSplitsStorage.getAll();
        for (Split split : splitsInCache) {
            String splitName = split.name;
            String splitPrefix = getPrefix(splitName);
            if (!namesToKeep.contains(split.name) && (splitPrefix == null || !prefixesToKeep.contains(splitPrefix))) {
                splitsToDelete.add(splitName);
            }
        }
        if (splitsToDelete.size() > 0) {
            mSplitsStorage.delete(splitsToDelete);
        }
        return SplitTaskExecutionInfo.success(SplitTaskType.FILTER_SPLITS_CACHE);
    }

    private String getPrefix(String splitName) {
        int separatorIndex = splitName.indexOf(PREFIX_SEPARATOR);
        if (separatorIndex < 1) {
            return null;
        }
        return splitName.substring(0, separatorIndex);
    }

    private boolean queryStringHasChanged() {
        return !sanitizeString(mSplitsStorage.getFilterQueryString()).equals(sanitizeString(mSplitsFilterQueryString));
    }

    private String sanitizeString(String string) {
        return string != null ? string : "";
    }
}
