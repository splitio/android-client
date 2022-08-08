package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import java.net.URISyntaxException;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTaskConfig;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.utils.logger.Logger;

public class ImpressionsRecorderWorker extends SplitWorker {
    public ImpressionsRecorderWorker(@NonNull Context context,
                                     @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        try {
            int impressionsPerPush = workerParams.getInputData().getInt(
                    ServiceConstants.WORKER_PARAM_IMPRESSIONS_PER_PUSH,
                    ServiceConstants.DEFAULT_RECORDS_PER_PUSH);
            boolean shouldRecordTelemetry = workerParams.getInputData().getBoolean(
                    ServiceConstants.SHOULD_RECORD_TELEMETRY, false);

            ImpressionsRecorderTaskConfig config =
                    new ImpressionsRecorderTaskConfig(
                            impressionsPerPush,
                            ServiceConstants.ESTIMATED_IMPRESSION_SIZE_IN_BYTES,
                            shouldRecordTelemetry);

            mSplitTask = new ImpressionsRecorderTask(ServiceFactory.getImpressionsRecorder(
                    getNetworkHelper(), getHttpClient(), getEndPoint()),
                    StorageFactory.getPersistentImpressionsStorage(getDatabase()),
                    config,
                    StorageFactory.getTelemetryStorage(config.shouldRecordTelemetry()));
        } catch (URISyntaxException e) {
            Logger.e("Error creating Split worker: " + e.getMessage());
        }
    }
}
