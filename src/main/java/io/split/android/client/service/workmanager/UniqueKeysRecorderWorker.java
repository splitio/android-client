package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.impressions.unique.UniqueKeysRecorderTask;
import io.split.android.client.service.impressions.unique.UniqueKeysRecorderTaskConfig;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.utils.logger.Logger;

public class UniqueKeysRecorderWorker extends SplitWorker {

    public UniqueKeysRecorderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        try {
            Data inputData = workerParams.getInputData();
            mSplitTask = new UniqueKeysRecorderTask(ServiceFactory.getUniqueKeysRecorder(getNetworkHelper(),
                    getHttpClient(),
                    getEndPoint()),
                    StorageFactory.getPersistentImpressionsUniqueStorage(getDatabase()),
                    new UniqueKeysRecorderTaskConfig(
                            inputData.getInt(ServiceConstants.WORKER_PARAM_UNIQUE_KEYS_PER_PUSH, ServiceConstants.DEFAULT_RECORDS_PER_PUSH),
                            inputData.getLong(ServiceConstants.WORKER_PARAM_UNIQUE_KEYS_ESTIMATED_SIZE_IN_BYTES, ServiceConstants.ESTIMATED_IMPRESSION_SIZE_IN_BYTES)
                    ));
        } catch (Exception e) {
            Logger.e("Error creating unique keys Split worker: " + e.getMessage());
        }
    }
}
