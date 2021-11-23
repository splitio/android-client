package io.split.android.client.validators;

import java.util.Collection;

public class AttributesValidatorImpl implements AttributesValidator {

    @Override
    public boolean isValid(Object attribute) {
        return  attribute instanceof String ||
                attribute instanceof Boolean ||
                attribute instanceof Integer ||
                attribute instanceof Long ||
                attribute instanceof Float ||
                attribute instanceof Double ||
                attribute instanceof Collection;
    }
}
