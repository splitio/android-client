package io.split.android.client.validators;

import java.util.List;

public interface SplitFilterValidator {

    ValidationResult cleanup(List<String> values);

    boolean isValid(String value);

    class ValidationResult {

        private final List<String> mValues;

        private final int mInvalidValueCount;

        public ValidationResult(List<String> values, int invalidValueCount) {
            mValues = values;
            mInvalidValueCount = invalidValueCount;
        }

        public List<String> getValues() {
            return mValues;
        }

        public int getInvalidValueCount() {
            return mInvalidValueCount;
        }
    }
}
