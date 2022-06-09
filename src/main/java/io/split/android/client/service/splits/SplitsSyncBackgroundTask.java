package io.split.android.client.service.splits;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.storage.splits.SplitsStorage;

public class SplitsSyncBackgroundTask implements SplitTask {

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
        return mSplitsSyncHelper.sync(storedChangeNumber, cleanBeforeUpdate, false, false);
    }
}
