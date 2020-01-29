package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import io.split.android.client.service.executor.SplitTask;

public class ImpressionsRecorderWorker extends SplitWorker {
    public ImpressionsRecorderWorker(@NonNull Context context,
                                     @NonNull WorkerParameters workerParams,
                                     @NonNull SplitTask task) {
        super(context, workerParams, task);
    }
}
