package io.split.android.client.service.workmanager.splits;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.work.WorkerParameters;

import io.split.android.client.service.workmanager.SplitWorker;

public class SplitsSyncWorker extends SplitWorker {

    @WorkerThread
    public SplitsSyncWorker(@NonNull Context context,
                            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        SplitsSyncWorkerParams params = new SplitsSyncWorkerParams(workerParams);

        SplitsSyncWorkerTaskBuilder builder = new SplitsSyncWorkerTaskBuilder(
                new StorageProvider(getDatabase(), params.apiKey(), params.encryptionEnabled(), params.shouldRecordTelemetry()),
                new FetcherProvider(getHttpClient(), getEndPoint()),
                new SplitChangeProcessorProvider().provideSplitChangeProcessor(params.configuredFilterType(), params.configuredFilterValues()),
                new SyncHelperProvider(),
                getCacheExpirationInSeconds(),
                params.flagsSpec());

        mSplitTask = builder.getTask();
    }
}
