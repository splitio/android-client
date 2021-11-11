package io.split.android.client.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.validators.AttributesValidator;

public class AttributesClientImpl implements AttributesClient {

    private final AttributesStorage mAttributesStorage;
    private final AttributesValidator mAttributesValidator;

    public AttributesClientImpl(@NonNull AttributesStorage attributesStorage,
                                @NonNull AttributesValidator attributesValidator) {
        mAttributesStorage = checkNotNull(attributesStorage);
        mAttributesValidator = checkNotNull(attributesValidator);
    }

    @Override
    public boolean setAttribute(String attributeName, Object value) {
        boolean isValid = mAttributesValidator.isValid(value);

        if (isValid) {
            mAttributesStorage.set(attributeName, value);

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
            mAttributesStorage.set(attributes);

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
        mAttributesStorage.remove(attributeName);
    }

    @Override
    public void clearAttributes() {
        mAttributesStorage.clear();
    }
}
