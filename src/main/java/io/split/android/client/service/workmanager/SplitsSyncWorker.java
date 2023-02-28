package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.work.WorkerParameters;

import java.net.URISyntaxException;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.utils.logger.Logger;

public class SplitsSyncWorker extends SplitWorker {

    @WorkerThread
    public SplitsSyncWorker(@NonNull Context context,
                            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        try {
            boolean shouldRecordTelemetry = workerParams.getInputData().getBoolean(ServiceConstants.SHOULD_RECORD_TELEMETRY, false);

            SplitsStorage splitsStorage = StorageFactory.getSplitsStorage(getDatabase());
            // StorageFactory.getSplitsStorage creates a new storage instance, so it needs
            // to be populated by calling loadLocal
            splitsStorage.loadLocal();
            HttpFetcher<SplitChange> splitsFetcher = ServiceFactory.getSplitsFetcher(getHttpClient(),
                    getEndPoint(), splitsStorage.getSplitsFilterQueryString());

            TelemetryStorage telemetryStorage = StorageFactory.getTelemetryStorage(shouldRecordTelemetry);

            SplitsSyncHelper splitsSyncHelper = new SplitsSyncHelper(splitsFetcher, splitsStorage, new SplitChangeProcessor(), telemetryStorage);

            mSplitTask = buildSplitSyncTask(splitsStorage, telemetryStorage, splitsSyncHelper);
        } catch (URISyntaxException e) {
            Logger.e("Error creating Split worker: " + e.getMessage());
        }
    }

    @NonNull
    private SplitTask buildSplitSyncTask(SplitsStorage splitsStorage, TelemetryStorage telemetryStorage, SplitsSyncHelper splitsSyncHelper) {
        return SplitsSyncTask.buildForBackground(splitsSyncHelper,
                splitsStorage,
                false,
                getCacheExpirationInSeconds(),
                splitsStorage.getSplitsFilterQueryString(),
                telemetryStorage);
    }
}
