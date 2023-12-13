package io.split.android.client.service.attributes;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.Map;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class UpdateAttributesInPersistentStorageTask implements SplitTask {

    private final String mMatchingKey;
    private final PersistentAttributesStorage mPersistentAttributesStorage;
    private final Map<String, Object> mAttributes;

    public UpdateAttributesInPersistentStorageTask(@NonNull String matchingKey, @NonNull PersistentAttributesStorage persistentAttributesStorage, @NonNull Map<String, Object> attributes) {
        mMatchingKey = checkNotNull(matchingKey);
        mPersistentAttributesStorage = checkNotNull(persistentAttributesStorage);
        mAttributes = checkNotNull(attributes);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        mPersistentAttributesStorage.set(mMatchingKey, mAttributes);

        return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
    }
}
