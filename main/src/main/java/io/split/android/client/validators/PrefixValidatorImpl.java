package io.split.android.client.validators;

import androidx.annotation.Nullable;

public class PrefixValidatorImpl implements PrefixValidator {

    private static final String PREFIX_REGEX = "^[a-zA-Z0-9_]{1,80}$";

    @Nullable
    @Override
    public ValidationErrorInfo validate(@Nullable String prefix) {
        if (prefix == null) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "You passed a null prefix, prefix must be a non-empty string");
        }

        if (prefix.trim().isEmpty()) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "You passed an empty prefix, prefix must be a non-empty string");
        }

        if (!prefix.trim().matches(PREFIX_REGEX)) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "Prefix can only contain alphanumeric characters and underscore, and must be 80 characters or less");
        }

        return null;
    }
}
