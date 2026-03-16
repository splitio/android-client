package io.split.android.client.service.impressions;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.HttpRecorderSubmitterAdapter;
import io.split.android.client.service.TelemetryRecorderAdapter;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.submitter.RecorderTask;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class ImpressionsRecorderTask extends RecorderTask<KeyImpression, List<KeyImpression>> {

    private final long mEstimatedSizeInBytes;

    public ImpressionsRecorderTask(@NonNull HttpRecorder<List<KeyImpression>> httpRecorder,
                                   @NonNull PersistentImpressionsStorage storage,
                                   @NonNull ImpressionsRecorderTaskConfig config,
                                   @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        super(storage,
                new HttpRecorderSubmitterAdapter<>(httpRecorder),
                config.getImpressionsPerPush(),
                SplitTaskType.IMPRESSIONS_RECORDER,
                new TelemetryRecorderAdapter(telemetryRuntimeProducer, OperationType.IMPRESSIONS),
                0);
        this.mEstimatedSizeInBytes = config.getEstimatedSizeInBytes();
    }

    @Override
    protected List<KeyImpression> transformForSubmission(List<KeyImpression> items) {
        return items;
    }

    @Override
    protected long estimateItemSize(KeyImpression item) {
        return mEstimatedSizeInBytes;
    }
}
