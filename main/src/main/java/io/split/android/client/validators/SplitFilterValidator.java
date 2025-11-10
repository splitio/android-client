package io.split.android.client.validators;

import java.util.List;
import java.util.Set;

import io.split.android.client.FlagSetsFilter;

public interface SplitFilterValidator {

    ValidationResult cleanup(String method, List<String> values);

    boolean isValid(String value);

    Set<String> items(String method, List<String> values, FlagSetsFilter flagSetsFilter);

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
