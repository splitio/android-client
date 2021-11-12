package io.split.android.client.service.attributes;

import androidx.annotation.NonNull;

import java.util.Map;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;

public class UpdateAttributesTask implements SplitTask {

    private final AttributesStorage mAttributesStorage;
    private final Map<String, Object> mAttributes;

    public UpdateAttributesTask(AttributesStorage attributesStorage, @NonNull Map<String, Object> attributes) {
        mAttributesStorage = attributesStorage;
        mAttributes = attributes;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        mAttributesStorage.set(mAttributes);

        return SplitTaskExecutionInfo.success(SplitTaskType.UPDATE_LOCAL_ATTRIBUTES);
    }
}
