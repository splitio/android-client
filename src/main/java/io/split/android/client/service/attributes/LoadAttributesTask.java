package io.split.android.client.service.attributes;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class LoadAttributesTask implements SplitTask {

    private final AttributesStorage mAttributesStorage;
    private final PersistentAttributesStorage mPersistentAttributesStorage;

    public LoadAttributesTask(@NonNull AttributesStorage attributesStorage, @NonNull PersistentAttributesStorage persistentAttributesStorage) {
        mAttributesStorage = attributesStorage;
        mPersistentAttributesStorage = persistentAttributesStorage;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        mAttributesStorage.set(mPersistentAttributesStorage.getAll());
        return SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_ATTRIBUTES);
    }
}
