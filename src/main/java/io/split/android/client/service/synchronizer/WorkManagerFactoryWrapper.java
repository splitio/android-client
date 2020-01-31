package io.split.android.client.service.synchronizer;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.workmanager.SplitWorkerFactory;

@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
public class WorkManagerFactoryWrapper {

    private final WorkManager mWorkManager;

    public WorkManagerFactoryWrapper(Context context,
                                     SplitTaskFactory splitTaskFactory) {

        Configuration workManagerConfig = new Configuration.Builder()
                .setWorkerFactory(new SplitWorkerFactory(splitTaskFactory))
                .build();
        WorkManager.initialize(context, workManagerConfig);
        mWorkManager = WorkManager.getInstance(context);
    }

    public WorkManager getWorkManager() {
        return mWorkManager;
    }
}
