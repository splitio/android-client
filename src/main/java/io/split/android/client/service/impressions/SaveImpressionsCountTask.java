package io.split.android.client.service.impressions;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.impressions.PersistentImpressionsCountStorage;
import io.split.android.client.storage.splits.SplitsStorage;

import static com.google.common.base.Preconditions.checkNotNull;

public class SaveImpressionsCountTask implements SplitTask {

    private final PersistentImpressionsCountStorage mCountsStorage;
    private final List<ImpressionsCountPerFeature> mCounts;

    public SaveImpressionsCountTask(@NonNull PersistentImpressionsCountStorage countsStorage,
                                    @NonNull ImpressionsCount counts) {
        mCountsStorage = checkNotNull(countsStorage);
        mCounts = checkNotNull(counts.perFeature);
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        mCountsStorage.pushMany(mCounts);
        return SplitTaskExecutionInfo.success(SplitTaskType.SAVE_IMPRESSIONS_COUNT);
    }
}
