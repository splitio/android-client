package io.split.android.client.service.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.Map;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;

public abstract class GenericUpdateAttributeTask implements SplitTask {

    private final AttributesStorage mAttributesStorage;
    private final String mAttributeName;
    private final Object mAttributeValue;
    private final Map<String, Object> mAttributes;

    public GenericUpdateAttributeTask(@NonNull AttributesStorage attributesStorage,
                                      @NonNull String attributeName,
                                      @NonNull Object attributeValue) {
        mAttributesStorage = checkNotNull(attributesStorage);
        mAttributeName = checkNotNull(attributeName);
        mAttributeValue = checkNotNull(attributeValue);
        mAttributes = null;
    }

    public GenericUpdateAttributeTask(@NonNull AttributesStorage attributesStorage,
                                      @NonNull Map<String, Object> attributes) {
        mAttributesStorage = checkNotNull(attributesStorage);
        mAttributes = checkNotNull(attributes);
        mAttributeName = null;
        mAttributeValue = null;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        if (mAttributes != null) {
            mAttributesStorage.set(mAttributes);
        } else {
            mAttributesStorage.set(mAttributeName, mAttributeValue);
        }

        return SplitTaskExecutionInfo.success(SplitTaskType.UPDATE_LOCAL_ATTRIBUTES);
    }
}
