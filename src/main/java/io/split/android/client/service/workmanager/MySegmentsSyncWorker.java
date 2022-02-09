package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import java.net.URISyntaxException;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.utils.Logger;

public class MySegmentsSyncWorker extends SplitWorker {
    public MySegmentsSyncWorker(@NonNull Context context,
                                @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        String key =
                workerParams.getInputData().getString(ServiceConstants.WORKER_PARAM_KEY);
        boolean shouldRecordTelemetry = workerParams.getInputData().getBoolean(ServiceConstants.SHOULD_RECORD_TELEMETRY, false);
        try {
            mSplitTask = new MySegmentsSyncTask(
                    ServiceFactory.getMySegmentsFetcher(getNetworkHelper(), getHttpClient(),
                            getEndPoint(), key),
                    StorageFactory.getMySegmentsStorage(getDatabase()).getStorageForKey(key),
                    false,
                    null,
                    StorageFactory.getTelemetryStorage(shouldRecordTelemetry));
        } catch (URISyntaxException e) {
            Logger.e("Error creating Split worker: " + e.getMessage());
        }
    }
}
