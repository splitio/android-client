package io.split.android.client.service.impressions.unique;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.storage.impressions.PersistentImpressionsUniqueStorage;
import io.split.android.client.utils.Logger;

public class UniqueKeysRecorderTask implements SplitTask {

    private final HttpRecorder<MTK> mHttpRecorder;
    private final PersistentImpressionsUniqueStorage mStorage;
    private final UniqueKeysRecorderTaskConfig mConfig;

    public UniqueKeysRecorderTask(@NonNull HttpRecorder<MTK> uniqueImpressionsRecorder,
                                  @NonNull PersistentImpressionsUniqueStorage storage,
                                  @NonNull UniqueKeysRecorderTaskConfig config) {
        mHttpRecorder = checkNotNull(uniqueImpressionsRecorder);
        mStorage = checkNotNull(storage);
        mConfig = checkNotNull(config);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        SplitTaskExecutionStatus status = SplitTaskExecutionStatus.SUCCESS;
        int nonSentRecords = 0;
        long nonSentBytes = 0;
        List<UniqueKey> keys;
        List<UniqueKey> failingKeys = new ArrayList<>();
        do {
            keys = mStorage.pop(mConfig.getElementsPerPush());
            if (keys.size() > 0) {
                try {
                    Logger.d("Posting %d Split MTKs", keys.size());
                    mHttpRecorder.execute(buildMTK(keys));

                    mStorage.delete(keys);
                    Logger.d("%d split MTKs sent", keys.size());
                } catch (HttpRecorderException e) {
                    status = SplitTaskExecutionStatus.ERROR;
                    nonSentRecords += mConfig.getElementsPerPush();
                    nonSentBytes += sumImpressionsBytes(keys);
                    Logger.e("MTKs recorder task: Some keys couldn't be sent." +
                            "Saving to send them in a new iteration" +
                            e.getLocalizedMessage());
                    failingKeys.addAll(keys);
                }
            }
        } while (keys.size() == mConfig.getElementsPerPush());

        if (failingKeys.size() > 0) {
            mStorage.setActive(failingKeys);
        }

        if (status == SplitTaskExecutionStatus.ERROR) {
            Map<String, Object> data = new HashMap<>();
            data.put(SplitTaskExecutionInfo.NON_SENT_RECORDS, nonSentRecords);
            data.put(SplitTaskExecutionInfo.NON_SENT_BYTES, nonSentBytes);

            return SplitTaskExecutionInfo.error(
                    SplitTaskType.UNIQUE_KEYS_RECORDER_TASK, data);
        }

        return SplitTaskExecutionInfo.success(SplitTaskType.UNIQUE_KEYS_RECORDER_TASK);
    }

    @NonNull
    private static MTK buildMTK(List<UniqueKey> keys) {
        Map<String, UniqueKey> map = new HashMap<>();
        for (UniqueKey key : keys) {
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

    private long sumImpressionsBytes(List<UniqueKey> keys) {
        long totalBytes = 0;
        for (UniqueKey key : keys) {
            totalBytes += mConfig.getEstimatedSizeInBytes();
        }
        return totalBytes;
    }
}
