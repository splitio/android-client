package io.split.android.client.service.splits;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitsUpdateTask implements SplitTask {

    static final String SINCE_PARAM = "since";
    private final SplitsStorage mSplitsStorage;
    private Long mChangeNumber;
    private final SplitsSyncHelper mSplitsSyncHelper;

    public SplitsUpdateTask(SplitsSyncHelper splitsSyncHelper,
                            SplitsStorage splitsStorage,
                            long since) {
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitsSyncHelper = checkNotNull(splitsSyncHelper);
        mChangeNumber = since;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {

        if (mChangeNumber == null || mChangeNumber == 0) {
            Logger.e("Could not update split. Invalid change number " + mChangeNumber);
            return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
        }

        long storedChangeNumber = mSplitsStorage.getTill();
        if (mChangeNumber <= storedChangeNumber) {
            Logger.d("Received change number is previous than stored one. " +
                    "Avoiding update.");
            return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
        }

        Map<String, Object> params = new HashMap<>();
        params.put(SINCE_PARAM, storedChangeNumber);

        return mSplitsSyncHelper.sync(params, false);
    }
}
