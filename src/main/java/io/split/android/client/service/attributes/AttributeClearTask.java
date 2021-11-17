package io.split.android.client.service.attributes;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class AttributeClearTask implements SplitTask {

    private final PersistentAttributesStorage mPersistentAttributesStorage;

    public AttributeClearTask(PersistentAttributesStorage persistentAttributesStorage) {
        mPersistentAttributesStorage = persistentAttributesStorage;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        mPersistentAttributesStorage.clear();

        return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
    }
}
