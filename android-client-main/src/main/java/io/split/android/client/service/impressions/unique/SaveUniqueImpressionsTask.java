package io.split.android.client.service.impressions.unique;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.impressions.PersistentImpressionsUniqueStorage;

public class SaveUniqueImpressionsTask implements SplitTask {

    private final PersistentImpressionsUniqueStorage mStorage;
    private final Map<String, Set<String>> mUniqueKeys;

    public SaveUniqueImpressionsTask(@NonNull PersistentImpressionsUniqueStorage storage,
                                     Map<String, Set<String>> uniqueKeys) {
        mStorage = checkNotNull(storage);
        mUniqueKeys = (uniqueKeys == null) ? Collections.emptyMap() : uniqueKeys;
    }

    @NonNull
    @WorkerThread
    @Override
    public SplitTaskExecutionInfo execute() {
        if (!mUniqueKeys.isEmpty()) {
            mStorage.pushMany(mapToModel(mUniqueKeys));
        }

        return SplitTaskExecutionInfo.success(SplitTaskType.SAVE_UNIQUE_KEYS_TASK);
    }

    @NonNull
    private static List<UniqueKey> mapToModel(Map<String, Set<String>> uniqueKeysMap) {
        List<UniqueKey> uniqueKeyList = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : uniqueKeysMap.entrySet()) {
            uniqueKeyList.add(new UniqueKey(entry.getKey(), entry.getValue()));
        }
        return uniqueKeyList;
    }
}
