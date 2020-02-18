package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskFactoryImpl;

public class MySegmentsSyncWorker extends SplitWorker {
    public MySegmentsSyncWorker(@NonNull Context context,
                                @NonNull WorkerParameters workerParams) {
        super(context, workerParams, SplitTaskFactoryImpl.getInstance().createMySegmentsSyncTask());
    }
}
