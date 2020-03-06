package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import java.net.URISyntaxException;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskFactoryImpl;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.utils.Logger;

public class MySegmentsSyncWorker extends SplitWorker {
    public MySegmentsSyncWorker(@NonNull Context context,
                                @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        String key =
                workerParams.getInputData().getString(ServiceConstants.WORKER_PARAM_KEY);
        try {
            mSplitTask = new MySegmentsSyncTask(
                    ServiceFactory.getMySegmentsFetcher(getNetworkHelper(), getHttpClient(),
                            getEndPoint(), key, getMetrics()),
                    StorageFactory.getMySegmentsStorage(getDatabase(), key));
        } catch (URISyntaxException e) {
            Logger.e("Error creating Split worker: " + e.getMessage());
        }
    }
}
