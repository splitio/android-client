package io.split.android.client.validators;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.SplitResult;
import io.split.android.grammar.Treatments;

class TreatmentManagerHelper {

    static <T> Map<String, T> controlTreatmentsForSplitsWithConfig(SplitValidator splitValidator, ValidationMessageLogger validationLogger, List<String> splits, String validationTag, TreatmentManagerImpl.ResultTransformer<T> resultTransformer) {
        Map<String, T> results = new HashMap<>();
        for (String split : splits) {
            if (isInvalidSplit(splitValidator, validationTag, validationLogger, split)) {
                continue;
            }

            results.put(split.trim(), resultTransformer.transform(new SplitResult(Treatments.CONTROL)));
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
}
