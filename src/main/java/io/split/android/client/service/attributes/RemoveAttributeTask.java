package io.split.android.client.service.attributes;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;

public class RemoveAttributeTask implements SplitTask {

    private final AttributesStorage mAttributesStorage;
    private final String mAttributeName;

    public RemoveAttributeTask(@NonNull AttributesStorage attributesStorage, @NonNull String attributeName) {
        mAttributesStorage = attributesStorage;
        mAttributeName = attributeName;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        mAttributesStorage.remove(mAttributeName);
        return SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_ATTRIBUTES);
    }
}
