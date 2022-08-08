package io.split.android.client.service.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class LoadAttributesTask implements SplitTask {

    private final String mMatchingKey;
    @Nullable
    private final AttributesStorage mAttributesStorage;
    @Nullable
    private final PersistentAttributesStorage mPersistentAttributesStorage;

    public LoadAttributesTask(@NonNull String matchingKey, @Nullable AttributesStorage attributesStorage, @Nullable PersistentAttributesStorage persistentAttributesStorage) {
        mMatchingKey = checkNotNull(matchingKey);
        mAttributesStorage = attributesStorage;
        mPersistentAttributesStorage = persistentAttributesStorage;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        if (mPersistentAttributesStorage != null && mAttributesStorage != null) {
            mAttributesStorage.set(mPersistentAttributesStorage.getAll(mMatchingKey));
        }
        return SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_ATTRIBUTES);
    }
}
