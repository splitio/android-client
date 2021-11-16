package io.split.android.client.service.attributes;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;

public abstract class GenericRemoveAttributeTask implements SplitTask {

    private final AttributesStorage mAttributesStorage;
    private final String mAttributeName;

    public GenericRemoveAttributeTask(@NonNull AttributesStorage attributesStorage) {
        mAttributesStorage = attributesStorage;
        mAttributeName = null;
    }

    public GenericRemoveAttributeTask(@NonNull AttributesStorage attributesStorage,
                                      @NonNull String attributeName) {
        mAttributesStorage = attributesStorage;
        mAttributeName = attributeName;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        if (mAttributeName != null) {
            mAttributesStorage.remove(mAttributeName);
            return SplitTaskExecutionInfo.success(SplitTaskType.REMOVE_LOCAL_ATTRIBUTE);
        } else {
            mAttributesStorage.clear();
            return SplitTaskExecutionInfo.success(SplitTaskType.CLEAR_LOCAL_ATTRIBUTES);
        }
    }
}
