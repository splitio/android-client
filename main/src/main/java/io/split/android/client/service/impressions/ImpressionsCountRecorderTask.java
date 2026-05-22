package io.split.android.client.service.impressions;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.service.HttpRecorderSubmitterAdapter;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.TelemetryRecorderAdapter;
import io.split.android.client.service.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.storage.impressions.PersistentImpressionsCountStorage;
import io.split.android.client.submitter.RecorderTask;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class ImpressionsCountRecorderTask extends RecorderTask<ImpressionsCountPerFeature, ImpressionsCount> {

    public ImpressionsCountRecorderTask(@NonNull HttpRecorder<ImpressionsCount> httpRecorder,
                                        @NonNull PersistentImpressionsCountStorage persistentStorage,
                                        @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        super(persistentStorage,
                new HttpRecorderSubmitterAdapter<>(httpRecorder),
                ServiceConstants.DEFAULT_IMPRESSION_COUNT_ROWS_POP,
                SplitTaskType.IMPRESSIONS_COUNT_RECORDER,
                new TelemetryRecorderAdapter(telemetryRuntimeProducer, OperationType.IMPRESSIONS_COUNT),
                0);
    }

    @Override
    protected ImpressionsCount transformForSubmission(List<ImpressionsCountPerFeature> items) {
        return new ImpressionsCount(items);
    }
}
