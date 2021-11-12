package io.split.android.client.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.validators.AttributesValidator;

public class AttributesClientImpl implements AttributesClient {

    private final AttributesStorage mAttributesStorage;
    private final AttributesValidator mAttributesValidator;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitTaskFactory mSplitTaskFactory;

    public AttributesClientImpl(@NonNull AttributesStorage attributesStorage,
                                @NonNull AttributesValidator attributesValidator,
                                @NonNull SplitTaskFactory splitTaskFactory,
                                @NonNull SplitTaskExecutor splitTaskExecutor) {
        mAttributesStorage = checkNotNull(attributesStorage);
        mAttributesValidator = checkNotNull(attributesValidator);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
    }

    @Override
    public boolean setAttribute(String attributeName, Object value) {
        boolean isValid = mAttributesValidator.isValid(value);

        if (isValid) {
            mSplitTaskExecutor.submit(mSplitTaskFactory.createUpdateSingleAttributeTask(attributeName, value), null);

            return true;
        } else {
            return false;
        }
    }

    @Nullable
    @Override
    public Object getAttribute(String attributeName) {
        return mAttributesStorage.get(attributeName);
    }

    @Override
    public boolean setAttributes(Map<String, Object> attributes) {
        boolean isValid = mAttributesValidator.isValid(attributes.values());

        if (isValid) {
            mSplitTaskExecutor.submit(mSplitTaskFactory.createUpdateAttributesTask(attributes), null);

            return true;
        } else {
            return false;
        }
    }

    @NonNull
    @Override
    public Map<String, Object> getAllAttributes() {
        return mAttributesStorage.getAll();
    }

    @Override
    public void removeAttribute(String attributeName) {
        mSplitTaskExecutor.submit(mSplitTaskFactory.createRemoveAttributeTask(attributeName), null);
    }

    @Override
    public void clearAttributes() {
        mSplitTaskExecutor.submit(mSplitTaskFactory.createClearAttributesTask(), null);
    }
}
