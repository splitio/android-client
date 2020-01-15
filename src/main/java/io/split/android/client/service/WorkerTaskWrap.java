package io.split.android.client.service;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;

public class WorkerTaskWrap extends Worker {
    private final static String TASK_STATUS = "taskStatus";
    private final static String RECORDS_NON_SENT = "recordNonSent";
    private final static String BYTES_NON_SET = "bytesNonSent";

    SplitTask mSplitTask;
    public WorkerTaskWrap(@NonNull Context context,
                          @NonNull WorkerParameters workerParams,
                          @NonNull SplitTask task) {

        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SplitTaskExecutionInfo info = mSplitTask.execute();
        Data data = new Data.Builder()
                .putInt(TASK_STATUS, info.)
                .putInt(RECORDS_NON_SENT, info.getNonSentRecords()
                .putInt(BYTES_NON_SET, info.getNonSentBytes()
                .build();
        return null;
    }

}
