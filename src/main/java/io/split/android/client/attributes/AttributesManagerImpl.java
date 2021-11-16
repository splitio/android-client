package io.split.android.client.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.validators.AttributesValidator;
import io.split.android.client.validators.ValidationMessageLogger;

public class AttributesManagerImpl implements AttributesManager {

    private final AttributesStorage mAttributesStorage;
    private final AttributesValidator mAttributesValidator;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitTaskFactory mSplitTaskFactory;
    private final ValidationMessageLogger mValidationMessageLogger;

    public AttributesManagerImpl(@NonNull AttributesStorage attributesStorage,
                                 @NonNull AttributesValidator attributesValidator,
                                 @NonNull SplitTaskFactory splitTaskFactory,
                                 @NonNull SplitTaskExecutor splitTaskExecutor,
                                 @NonNull ValidationMessageLogger validationMessageLogger) {
        mAttributesStorage = checkNotNull(attributesStorage);
        mAttributesValidator = checkNotNull(attributesValidator);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mValidationMessageLogger = checkNotNull(validationMessageLogger);
    }

    @Override
    public boolean setAttribute(String attributeName, Object value) {
        if (!mAttributesValidator.isValid(value)) {
            logValidationWarning(attributeName);
            return false;
        }

        mSplitTaskExecutor.submit(mSplitTaskFactory.createUpdateSingleAttributeTask(attributeName, value), null);

        return true;
    }

    @Nullable
    @Override
    public Object getAttribute(String attributeName) {
        return mAttributesStorage.get(attributeName);
    }

    @Override
    public boolean setAttributes(Map<String, Object> attributes) {
        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            if (!mAttributesValidator.isValid(attribute.getValue())) {
                logValidationWarning(attribute.getKey());
                return false;
            }
        }

        mSplitTaskExecutor.submit(mSplitTaskFactory.createUpdateAttributesTask(attributes), null);

        return true;
    }

    private void logValidationWarning(String key) {
        String mValidationTag = "split attributes";
        mValidationMessageLogger.w("You passed an invalid attribute value for " + key + ", acceptable types are String, double, float, long, int, boolean or Collections", mValidationTag);
    }

    @NonNull
    @Override
    public Map<String, Object> getAllAttributes() {
        return mAttributesStorage.getAll();
    }

    @Override
    public boolean removeAttribute(String attributeName) {
        mSplitTaskExecutor.submit(mSplitTaskFactory.createRemoveAttributeTask(attributeName), null);

        return true;
    }

    @Override
    public boolean clearAttributes() {
        mSplitTaskExecutor.submit(mSplitTaskFactory.createClearAttributesTask(), null);

        return true;
    }
}
