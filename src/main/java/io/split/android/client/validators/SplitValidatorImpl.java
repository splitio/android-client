package io.split.android.client.validators;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.api.Key;
import io.split.android.client.dtos.Split;

/**
 * Implementation of split validation interface
 */
public class SplitValidatorImpl implements SplitValidator {

    @Override
    public ValidationErrorInfo validateName(String name) {

        if (name == null) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed a null split name, split name must be a non-empty string");
        }

        if (Strings.isNullOrEmpty(name.trim())) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed an empty split name, split name must be a non-empty string");
        }

        if(name.trim().length() != name.length()) {
            return new ValidationErrorInfo(ValidationErrorInfo.WARNING_SPLIT_NAME_SHOULD_BE_TRIMMED, "split name '" + name + "' has extra whitespace, trimming", true);
        }

        return null;
    }
}
