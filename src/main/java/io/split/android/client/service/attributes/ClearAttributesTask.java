package io.split.android.client.service.attributes;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;

public class ClearAttributesTask extends GenericRemoveAttributeTask {

    public ClearAttributesTask(@NonNull AttributesStorage attributesStorage) {
        super(attributesStorage);
    }
}
