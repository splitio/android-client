package io.split.android.client.service.attributes;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;

public class ClearAttributesTask implements SplitTask {

    private final AttributesStorage mAttributesStorage;

    public ClearAttributesTask(@NonNull AttributesStorage attributesStorage) {
        mAttributesStorage = attributesStorage;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        mAttributesStorage.clear();

        return SplitTaskExecutionInfo.success(SplitTaskType.CLEAR_LOCAL_ATTRIBUTES);
    }
}
