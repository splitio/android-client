package io.split.android.client.service.attributes;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;

public class RemoveAttributeTask extends GenericRemoveAttributeTask {

    public RemoveAttributeTask(@NonNull AttributesStorage attributesStorage, @NonNull String attributeName) {
        super(attributesStorage, attributeName);
    }
}
