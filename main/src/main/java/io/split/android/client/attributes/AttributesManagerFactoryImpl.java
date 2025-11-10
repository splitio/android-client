package io.split.android.client.attributes;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.Nullable;

import io.split.android.client.service.attributes.AttributeTaskFactoryImpl;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.validators.AttributesValidator;
import io.split.android.client.validators.ValidationMessageLogger;

public class AttributesManagerFactoryImpl implements AttributesManagerFactory {

    private final AttributesValidator mAttributesValidator;
    private final ValidationMessageLogger mValidationMessageLogger;
    @Nullable
    private final PersistentAttributesStorage mPersistentAttributesStorage;
    @Nullable
    private final SplitTaskExecutor mSplitTaskExecutor;

    public AttributesManagerFactoryImpl(AttributesValidator attributesValidator,
                                        ValidationMessageLogger validationMessageLogger) {
        this(attributesValidator, validationMessageLogger, null, null);
    }

    public AttributesManagerFactoryImpl(AttributesValidator attributesValidator,
                                        ValidationMessageLogger validationMessageLogger,
                                        @Nullable PersistentAttributesStorage persistentAttributesStorage,
                                        @Nullable SplitTaskExecutor splitTaskExecutor) {
        mAttributesValidator = checkNotNull(attributesValidator);
        mValidationMessageLogger = checkNotNull(validationMessageLogger);
        mPersistentAttributesStorage = persistentAttributesStorage;
        mSplitTaskExecutor = splitTaskExecutor;
    }

    @Override
    public AttributesManager getManager(String matchingKey, AttributesStorage attributesStorage) {
        return new AttributesManagerImpl(attributesStorage,
                mAttributesValidator,
                mValidationMessageLogger,
                mPersistentAttributesStorage,
                new AttributeTaskFactoryImpl(matchingKey, attributesStorage),
                mSplitTaskExecutor);
    }
}
