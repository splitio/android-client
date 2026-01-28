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
import io.split.android.client.service.mysegments.MySegmentsBulkSyncTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.logger.Logger;

abstract class BaseSegmentsSyncWorker extends SplitWorker {

    BaseSegmentsSyncWorker(@NonNull Context context,
                                @NonNull WorkerParameters workerParams) {

        super(context, workerParams);
        String[] keys =
                workerParams.getInputData().getStringArray(ServiceConstants.WORKER_PARAM_KEY);
        String apiKey = workerParams.getInputData().getString(ServiceConstants.WORKER_PARAM_API_KEY);
        boolean isEncryptionEnabled = workerParams.getInputData().getBoolean(ServiceConstants.WORKER_PARAM_ENCRYPTION_ENABLED,
                false);
        boolean shouldRecordTelemetry = workerParams.getInputData().getBoolean(ServiceConstants.SHOULD_RECORD_TELEMETRY, false);
        try {
            if (keys == null) {
                Logger.e("Error scheduling segments sync worker: Keys are null");
                return;
            }

            mSplitTask = new MySegmentsBulkSyncTask(Collections.unmodifiableSet(getIndividualMySegmentsSyncTasks(keys,
                    shouldRecordTelemetry,
                    getHttpClient(),
                    getEndPoint(),
                    getDatabase(),
                    apiKey,
                    isEncryptionEnabled)));

        } catch (URISyntaxException e) {
            Logger.e("Error creating Split worker: " + e.getMessage());
        }
    }

    private Set<MySegmentsSyncTask> getIndividualMySegmentsSyncTasks(String[] keys,
                                                                            boolean shouldRecordTelemetry,
                                                                            HttpClient httpClient,
                                                                            String endPoint,
                                                                            SplitRoomDatabase database,
                                                                            String apiKey,
                                                                            boolean isEncryptionEnabled) throws URISyntaxException {
        Set<MySegmentsSyncTask> mySegmentsSyncTasks = new HashSet<>();
        for (String key : keys) {
            mySegmentsSyncTasks.add(
                    getTask(shouldRecordTelemetry, httpClient, endPoint, database, apiKey, isEncryptionEnabled, key));
        }

        return mySegmentsSyncTasks;
    }

    protected abstract @NonNull MySegmentsSyncTask getTask(boolean shouldRecordTelemetry, HttpClient httpClient, String endPoint, SplitRoomDatabase database, String apiKey, boolean isEncryptionEnabled, String key) throws URISyntaxException;
}
