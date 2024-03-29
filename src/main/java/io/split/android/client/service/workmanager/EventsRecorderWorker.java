package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import java.net.URISyntaxException;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.ServiceFactory;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.events.EventsRecorderTaskConfig;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.utils.logger.Logger;

public class EventsRecorderWorker extends SplitWorker {
    public EventsRecorderWorker(@NonNull Context context,
                                @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        try {
            int eventsPerPush = workerParams.getInputData().getInt(
                    ServiceConstants.WORKER_PARAM_EVENTS_PER_PUSH,
                    ServiceConstants.DEFAULT_RECORDS_PER_PUSH);

            boolean shouldRecordTelemetry = workerParams.getInputData().getBoolean(
                    ServiceConstants.SHOULD_RECORD_TELEMETRY, false);
            String apiKey = workerParams.getInputData().getString(
                    ServiceConstants.WORKER_PARAM_API_KEY);
            boolean encryptionEnabled =
                    workerParams.getInputData().getBoolean(ServiceConstants.WORKER_PARAM_ENCRYPTION_ENABLED, false);

            mSplitTask = new EventsRecorderTask(ServiceFactory.getEventsRecorder(
                    getHttpClient(), getEndPoint()),
                    StorageFactory.getPersistentEventsStorageForWorker(getDatabase(), apiKey, encryptionEnabled),
                    new EventsRecorderTaskConfig(eventsPerPush),
                    StorageFactory.getTelemetryStorage(shouldRecordTelemetry));
        } catch (URISyntaxException e) {
            Logger.e("Error creating Split worker: " + e.getMessage());
        }
    }
}
