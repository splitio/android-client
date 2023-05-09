package io.split.android.client.validators;

import com.google.common.base.Strings;


import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.engine.experiments.SplitFetcher;

/**
 * Implementation of split validation interface
 */
public class SplitValidatorImpl implements SplitValidator {

    @Override
    public ValidationErrorInfo validateName(String name) {

        if (name == null) {
            return new ValidationErrorInfo(
                    ValidationErrorInfo.ERROR_SOME,
                    "you passed a null split name, split name must " +
                            "be a non-empty string");
        }

        if (Strings.isNullOrEmpty(name.trim())) {
            return new ValidationErrorInfo(
                    ValidationErrorInfo.ERROR_SOME,
                    "you passed an empty split name, " +
                            "split name must be a non-empty string");
        }

        if (name.trim().length() != name.length()) {
            return new ValidationErrorInfo(
                    ValidationErrorInfo.WARNING_SPLIT_NAME_SHOULD_BE_TRIMMED,
                    "split name '" + name + "' has extra whitespace, trimming",
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
