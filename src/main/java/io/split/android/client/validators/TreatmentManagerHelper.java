package io.split.android.client.validators;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.SplitResult;
import io.split.android.grammar.Treatments;

public class TreatmentManagerHelper {

    public static Map<String, SplitResult> controlTreatmentsForSplitsWithConfig(@NonNull List<String> splits, @Nullable String validationTag, @Nullable SplitValidator mSplitValidator, @Nullable ValidationMessageLogger mValidationLogger) {
        Map<String, SplitResult> results = new HashMap<>();
        for (String split : splits) {
            if (validate(validationTag, mSplitValidator, split, mValidationLogger)) continue;

            results.put(split.trim(), new SplitResult(Treatments.CONTROL));
        }

        return results;
    }

    public static Map<String, String> controlTreatmentsForSplits(@NonNull List<String> splits, @Nullable String validationTag, @Nullable SplitValidator mSplitValidator, @Nullable ValidationMessageLogger mValidationLogger) {
        Map<String, String> results = new HashMap<>();
        for (String split : splits) {
            if (validate(validationTag, mSplitValidator, split, mValidationLogger)) continue;

            results.put(split.trim(), Treatments.CONTROL);
        }

        return results;
    }

    public static Map<String, String> controlTreatmentsForSplits(@NonNull List<String> splits) {
        return controlTreatmentsForSplits(splits, null, null, null);
    }

    public static Map<String, SplitResult> controlTreatmentsForSplitsWithConfig(@NonNull List<String> splits) {
        return controlTreatmentsForSplitsWithConfig(splits, null, null, null);
    }

    private static boolean validate(String validationTag, SplitValidator mSplitValidator, String split, @Nullable ValidationMessageLogger mValidationLogger) {
        if (mSplitValidator != null) {
            ValidationErrorInfo errorInfo = mSplitValidator.validateName(split);
            if (errorInfo != null) {
                if (errorInfo.isError()) {
                    if (mValidationLogger != null) {
                        mValidationLogger.e(errorInfo, validationTag);
                    }
                    return true;
                }

                if (mValidationLogger != null) {
                    mValidationLogger.w(errorInfo, validationTag);
                }
            }
        }

        return false;
    }
}
