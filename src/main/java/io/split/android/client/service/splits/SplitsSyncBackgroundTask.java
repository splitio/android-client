package io.split.android.client.service.splits;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Thread.sleep;

public class SplitsSyncBackgroundTask implements SplitTask {

    static final String SINCE_PARAM = "since";
    private final SplitsSyncHelper mSplitsSyncHelper;
    private final SplitsStorage mSplitsStorage;
    private final long mCacheExpirationInSeconds;

    public SplitsSyncBackgroundTask(SplitsSyncHelper splitsSyncHelper,
                                    SplitsStorage splitsStorage,
                                    long cacheExpirationInSeconds) {
        mSplitsSyncHelper = checkNotNull(splitsSyncHelper);
        mSplitsStorage = checkNotNull(splitsStorage);
        mCacheExpirationInSeconds = cacheExpirationInSeconds;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        long storedChangeNumber = mSplitsStorage.getTill();
        boolean cleanBeforeUpdate = false;
        if (mSplitsSyncHelper.cacheHasExpired(storedChangeNumber, mSplitsStorage.getUpdateTimestamp(), mCacheExpirationInSeconds)) {
            cleanBeforeUpdate = true;
            storedChangeNumber = -1;
        }
        Map<String, Object> params = new HashMap<>();
        params.put(SINCE_PARAM, storedChangeNumber);
        return mSplitsSyncHelper.sync(params, cleanBeforeUpdate, false);
    }
}
