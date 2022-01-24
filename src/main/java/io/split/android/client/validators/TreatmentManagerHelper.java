package io.split.android.client.validators;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.SplitResult;
import io.split.android.grammar.Treatments;

public class TreatmentManagerHelper {

    public static Map<String, SplitResult> controlTreatmentsForSplitsWithConfig(@NonNull List<String> splits, @Nullable SplitValidator validator, @Nullable String validationTag, @Nullable ValidationMessageLogger logger) {
        Map<String, SplitResult> results = new HashMap<>();
        for (String split : splits) {
            if (isInvalidSplit(validator, validationTag, logger, split)) {
                continue;
            }

            results.put(split.trim(), new SplitResult(Treatments.CONTROL));
        }
        return results;
    }

    public static Map<String, String> controlTreatmentsForSplits(@NonNull List<String> splits, @Nullable SplitValidator validator, @Nullable String validationTag, @Nullable ValidationMessageLogger logger) {
        Map<String, String> results = new HashMap<>();
        for (String split : splits) {
            if (isInvalidSplit(validator, validationTag, logger, split)) {
                continue;
            }

            results.put(split.trim(), Treatments.CONTROL);
        }
        return results;
    }

    private static boolean isInvalidSplit(@Nullable SplitValidator validator, @Nullable String validationTag, @Nullable ValidationMessageLogger logger, String split) {
        if (validator != null) {
            ValidationErrorInfo errorInfo = validator.validateName(split);
            if (errorInfo != null) {
                if (errorInfo.isError()) {
                    if (logger != null) {
                        logger.e(errorInfo, validationTag);
                    }
                    return true;
                }

                if (logger != null) {
                    logger.w(errorInfo, validationTag);
                }
            }
        }
        return false;
    }

    public static Map<String, String> controlTreatmentsForSplits(@NonNull List<String> splits, SplitValidator validator) {
        return controlTreatmentsForSplits(splits, validator, null, null);
    }

    public static Map<String, SplitResult> controlTreatmentsForSplitsWithConfig(@NonNull List<String> splits, SplitValidator validator) {
        return controlTreatmentsForSplitsWithConfig(splits, validator, null, null);
    }

}
