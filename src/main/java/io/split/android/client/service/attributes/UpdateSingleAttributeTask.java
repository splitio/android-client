package io.split.android.client.service.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;

public class UpdateSingleAttributeTask implements SplitTask {

    private final AttributesStorage mAttributesStorage;
    private final String mAttributeName;
    private final Object mAttributeValue;

    public UpdateSingleAttributeTask(@NonNull AttributesStorage attributesStorage,
                                @NonNull String attributeName,
                                @NonNull Object attributeValue) {
        mAttributesStorage = checkNotNull(attributesStorage);
        mAttributeName = checkNotNull(attributeName);
        mAttributeValue = checkNotNull(attributeValue);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            mAttributesStorage.set(mAttributeName, mAttributeValue);

            return SplitTaskExecutionInfo.success(SplitTaskType.UPDATE_LOCAL_ATTRIBUTES);
        } catch (Exception exception) {
            return SplitTaskExecutionInfo.error(SplitTaskType.UPDATE_LOCAL_ATTRIBUTES);
        }
    }
}
