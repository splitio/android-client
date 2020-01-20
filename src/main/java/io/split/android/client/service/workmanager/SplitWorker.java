package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class SplitWorker extends Worker {


    SplitTask mSplitTask;
    public SplitWorker(@NonNull Context context,
                       @NonNull WorkerParameters workerParams,
                       @NonNull SplitTask task) {

        super(context, workerParams);
        mSplitTask = checkNotNull(task);
    }

    @NonNull
    @Override
    public Result doWork() {
        SplitTaskExecutionInfo info = mSplitTask.execute();
        Data data = new Data.Builder()
                .putString(ServiceConstants.TASK_INFO_FIELD_STATUS, info.getStatus().toString())
                .putString(ServiceConstants.TASK_INFO_FIELD_TYPE, info.getTaskType().toString())
                .putInt(ServiceConstants.TASK_INFO_FIELD_RECORDS_NON_SENT, info.getNonSentRecords())
                .putLong(ServiceConstants.TASK_INFO_FIELD_BYTES_NON_SET, info.getNonSentBytes())
                .build();
        return Result.success(data);
    }

}
