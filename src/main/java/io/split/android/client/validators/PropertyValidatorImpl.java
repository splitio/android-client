package io.split.android.client.validators;

import java.util.Map;

public class PropertyValidatorImpl implements PropertyValidator {

    @Override
    public Result validate(Map<String, Object> properties) {
        return Result.valid(properties, 0); // TODO implement
    }

    @Override
    public Result validate(Map<String, Object> properties, int initialSizeInBytes, String validationTag) {
        return Result.valid(properties, initialSizeInBytes);
    }
}
