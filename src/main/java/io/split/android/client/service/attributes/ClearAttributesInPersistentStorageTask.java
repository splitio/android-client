package io.split.android.client.service.attributes;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class ClearAttributesInPersistentStorageTask implements SplitTask {

    private final String mMatchingKey;
    private final PersistentAttributesStorage mPersistentAttributesStorage;

    public ClearAttributesInPersistentStorageTask(@NonNull String matchingKey, @NonNull PersistentAttributesStorage persistentAttributesStorage) {
        mMatchingKey = checkNotNull(matchingKey);
        mPersistentAttributesStorage = checkNotNull(persistentAttributesStorage);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        mPersistentAttributesStorage.clear(mMatchingKey);

        return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
    }
}
