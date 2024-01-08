package io.split.android.client.validators;

import io.split.android.client.utils.Utils;

/**
 * Implementation of split validation interface
 */
public class SplitValidatorImpl implements SplitValidator {

    @Override
    public ValidationErrorInfo validateName(String name) {

        if (name == null) {
            return new ValidationErrorInfo(
                    ValidationErrorInfo.ERROR_SOME,
                    "you passed a null feature flag name, flag name must " +
                            "be a non-empty string");
        }

        if (Utils.isNullOrEmpty(name.trim())) {
            return new ValidationErrorInfo(
                    ValidationErrorInfo.ERROR_SOME,
                    "you passed an empty feature flag name, " +
                            "flag name must be a non-empty string");
        }

        if (name.trim().length() != name.length()) {
            return new ValidationErrorInfo(
                    ValidationErrorInfo.WARNING_SPLIT_NAME_SHOULD_BE_TRIMMED,
                    "feature flag name '" + name + "' has extra whitespace, trimming",
                    true);
        }

        return null;
    }

    public String splitNotFoundMessage(String splitName) {
        return "split: you passed '" + splitName +
                "' that does not exist in this environment, " +
                "please double check what feature flags exist in the Split user interface.";
    }

}
