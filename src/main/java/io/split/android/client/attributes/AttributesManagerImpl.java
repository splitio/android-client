package io.split.android.client.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import io.split.android.client.service.attributes.AttributeTaskFactory;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.validators.AttributesValidator;
import io.split.android.client.validators.ValidationMessageLogger;

public class AttributesManagerImpl implements AttributesManager {

    private final AttributesStorage mAttributesStorage;
    private final AttributesValidator mAttributesValidator;
    private final ValidationMessageLogger mValidationMessageLogger;
    @Nullable private final PersistentAttributesStorage mPersistentAttributesStorage;
    @Nullable private final AttributeTaskFactory mAttributeTaskFactory;
    @Nullable private final SplitTaskExecutor mSplitTaskExecutor;

    public AttributesManagerImpl(@NonNull AttributesStorage attributesStorage,
                                 @NonNull AttributesValidator attributesValidator,
                                 @NonNull ValidationMessageLogger validationMessageLogger) {
        mAttributesStorage = checkNotNull(attributesStorage);
        mAttributesValidator = checkNotNull(attributesValidator);
        mValidationMessageLogger = checkNotNull(validationMessageLogger);
        mPersistentAttributesStorage = null;
        mAttributeTaskFactory = null;
        mSplitTaskExecutor = null;
    }

    public AttributesManagerImpl(@NonNull AttributesStorage attributesStorage,
                                 @NonNull AttributesValidator attributesValidator,
                                 @NonNull ValidationMessageLogger validationMessageLogger,
                                 @Nullable PersistentAttributesStorage persistentAttributesStorage,
                                 @Nullable AttributeTaskFactory attributeTaskFactory,
                                 @Nullable SplitTaskExecutor splitTaskExecutor) {
        mAttributesStorage = checkNotNull(attributesStorage);
        mAttributesValidator = checkNotNull(attributesValidator);
        mValidationMessageLogger = checkNotNull(validationMessageLogger);
        mPersistentAttributesStorage = persistentAttributesStorage;
        mAttributeTaskFactory = attributeTaskFactory;
        mSplitTaskExecutor = splitTaskExecutor;
    }

    @Override
    public boolean setAttribute(String attributeName, Object value) {
        if (!mAttributesValidator.isValid(value)) {
            logValidationWarning(attributeName);
            return false;
        }

        mAttributesStorage.set(attributeName, value);

        submitUpdateTask(mPersistentAttributesStorage, mAttributesStorage.getAll());

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

        mAttributesStorage.set(attributes);

        submitUpdateTask(mPersistentAttributesStorage, mAttributesStorage.getAll());

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
        mAttributesStorage.remove(attributeName);

        submitUpdateTask(mPersistentAttributesStorage, mAttributesStorage.getAll());

        return true;
    }

    @Override
    public boolean clearAttributes() {
        mAttributesStorage.clear();

        submitClearTask(mPersistentAttributesStorage);

        return true;
    }

    private void submitUpdateTask(PersistentAttributesStorage persistentStorage, Map<String, Object> mInMemoryAttributes) {
        if (persistentStorage != null && mSplitTaskExecutor != null && mAttributeTaskFactory != null) {
            mSplitTaskExecutor.submit(mAttributeTaskFactory.createAttributeUpdateTask(persistentStorage, mInMemoryAttributes), null);
        }
    }

    private void submitClearTask(PersistentAttributesStorage persistentStorage) {
        if (persistentStorage != null && mSplitTaskExecutor != null && mAttributeTaskFactory != null) {
            mSplitTaskExecutor.submit(mAttributeTaskFactory.createAttributeClearTask(persistentStorage), null);
        }
    }
}
