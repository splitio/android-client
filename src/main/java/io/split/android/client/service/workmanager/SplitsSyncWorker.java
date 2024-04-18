package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.work.WorkerParameters;

import io.split.android.client.FlagSetsFilterImpl;
import io.split.android.client.SplitFilter;
import io.split.android.client.service.splits.SplitChangeProcessor;

public class SplitsSyncWorker extends SplitWorker {

    @WorkerThread
    public SplitsSyncWorker(@NonNull Context context,
                            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        instantiateTask(workerParams);
    }

    private void instantiateTask(@NonNull WorkerParameters workerParams) {
        SplitsSyncWorkerParams params = new SplitsSyncWorkerParams(workerParams);
        mSplitTask = new SplitsSyncWorkerTaskBuilder(
                new SplitsSyncWorkerStorageProvider(getDatabase(), params.apiKey(), params.encryptionEnabled(), params.shouldRecordTelemetry()),
                new SplitsSyncWorkerFetcherProvider(getHttpClient(), getEndPoint()),
                getSplitChangeProcessor(params.configuredFilterType(), params.configuredFilterValues()),
                getCacheExpirationInSeconds(),
                params.flagsSpec()).getTask();
    }

    @NonNull
    private static SplitChangeProcessor getSplitChangeProcessor(String filterType, String[] filterValues) {
        SplitFilter filter = SplitsSyncWorkerFilterBuilder.buildFilter(filterType, filterValues);
        return new SplitChangeProcessor(filter, (filter != null && filter.getType() == SplitFilter.Type.BY_SET) ?
                new FlagSetsFilterImpl(filter.getValues()) : null);
    }
}
