package io.split.android.client.service.impressions.unique;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.service.HttpRecorderSubmitterAdapter;
import io.split.android.client.service.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.storage.impressions.PersistentImpressionsUniqueStorage;
import io.split.android.client.submitter.RecorderTask;

public class UniqueKeysRecorderTask extends RecorderTask<UniqueKey, MTK> {

    private final long mEstimatedSizeInBytes;

    public UniqueKeysRecorderTask(@NonNull HttpRecorder<MTK> uniqueImpressionsRecorder,
                                  @NonNull PersistentImpressionsUniqueStorage storage,
                                  @NonNull UniqueKeysRecorderTaskConfig config) {
        super(storage,
                new HttpRecorderSubmitterAdapter<>(uniqueImpressionsRecorder),
                config.getElementsPerPush(),
                SplitTaskType.UNIQUE_KEYS_RECORDER_TASK,
                null,
                0);
        this.mEstimatedSizeInBytes = config.getEstimatedSizeInBytes();
    }

    @Override
    protected MTK transformForSubmission(List<UniqueKey> items) {
        Map<String, UniqueKey> map = new HashMap<>();
        for (UniqueKey key : items) {
            String userKey = key.getKey();
            if (!map.containsKey(userKey)) {
                map.put(userKey, new UniqueKey(userKey, new HashSet<>()));
            }
            UniqueKey uniqueKey = map.get(userKey);
            if (uniqueKey != null) {
                Set<String> originalFeatures = uniqueKey.getFeatures();
                Set<String> newFeatures = key.getFeatures();
                newFeatures.addAll(originalFeatures);
                map.put(userKey, new UniqueKey(userKey, newFeatures));
            }
        }
        return new MTK(new ArrayList<>(map.values()));
    }

    @Override
    protected long estimateItemSize(UniqueKey item) {
        return mEstimatedSizeInBytes;
    }
}
