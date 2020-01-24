package io.split.android.client.service.mysegments;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class LoadMySegmentsTask implements SplitTask {

    private final MySegmentsStorage mMySegmentsStorage;

    public LoadMySegmentsTask(@NonNull MySegmentsStorage mySegmentsStorage) {

        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        mMySegmentsStorage.loadFromDisk();
        return SplitTaskExecutionInfo.success(SplitTaskType.LOAD_MY_SYGMENTS_FROM_DISK);
    }
}
