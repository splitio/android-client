package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.split.android.client.network.HttpClient;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.mysegments.MySegmentsBulkSyncTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.NetworkHelper;

public class MySegmentsSyncWorker extends SplitWorker {

    public MySegmentsSyncWorker(@NonNull Context context,
                                @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        String[] keys =
                workerParams.getInputData().getStringArray(ServiceConstants.WORKER_PARAM_KEY);
        boolean shouldRecordTelemetry = workerParams.getInputData().getBoolean(ServiceConstants.SHOULD_RECORD_TELEMETRY, false);
        try {
            if (keys == null) {
                return;
            }

            mSplitTask = new MySegmentsBulkSyncTask(Collections.unmodifiableSet(getIndividualMySegmentsSyncTasks(keys, shouldRecordTelemetry, getNetworkHelper(), getHttpClient(), getEndPoint(), getDatabase())));
        } catch (URISyntaxException e) {
            Logger.e("Error creating Split worker: " + e.getMessage());
        }
    }

    private static Set<MySegmentsSyncTask> getIndividualMySegmentsSyncTasks(String[] keys,
                                                                            boolean shouldRecordTelemetry,
                                                                            NetworkHelper networkHelper,
                                                                            HttpClient httpClient,
                                                                            String endPoint,
                                                                            SplitRoomDatabase database) throws URISyntaxException {
        Set<MySegmentsSyncTask> mySegmentsSyncTasks = new HashSet<>();
        for (String key : keys) {
            mySegmentsSyncTasks.add(
                    new MySegmentsSyncTask(
                            ServiceFactory.getMySegmentsFetcher(networkHelper, httpClient,
                                    endPoint, key),
                            StorageFactory.getMySegmentsStorage(database).getStorageForKey(key),
                            false,
                            null,
                            StorageFactory.getTelemetryStorage(shouldRecordTelemetry))
            );
        }

        return mySegmentsSyncTasks;
    }
}
