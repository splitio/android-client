package io.split.android.client.service.attributes;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;

public class LoadAttributesTask implements SplitTask {

    private final AttributesStorage mAttributesStorage;

    public LoadAttributesTask(@NonNull AttributesStorage attributesStorage) {
        mAttributesStorage = attributesStorage;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        mAttributesStorage.loadLocal();
        return SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_ATTRIBUTES);
    }
}
