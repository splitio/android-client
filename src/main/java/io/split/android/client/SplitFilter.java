package io.split.android.client;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.validators.FlagSetsValidatorImpl;
import io.split.android.client.validators.SplitFilterValidator;

public class SplitFilter {
    public enum Type {
        // Filters here has to be defined in the order
        // it will be in querystring
        BY_NAME,
        BY_PREFIX,
        BY_SET;

        @NonNull
        @Override
        public String toString() {
            switch (this) {
                case BY_NAME:
                    return "by split name";
                case BY_PREFIX:
                    return "by split prefix";
                case BY_SET:
                    return "by flag set";
                default:
                    return "Invalid type";
            }
        }

        public String queryStringField() {
            switch (this) {
                case BY_NAME:
                    return "names";
                case BY_PREFIX:
                    return "prefixes";
                case BY_SET:
                    return "sets";
                default:
                    return "unknown";
            }
        }

        public int maxValuesCount() {
            switch (this) {
                case BY_NAME:
                    return 400;
                case BY_PREFIX:
                    return 50;
                case BY_SET:
                    return 999999;
                default:
                    return 0;
            }
        }
    }

    private final SplitFilter.Type mType;
    private final List<String> mValues;
    private int mInvalidValueCount;
    private int mTotalValueCount;

    public static SplitFilter byName(@NonNull List<String> values) {
        return new SplitFilter(Type.BY_NAME, values);
    }

    public static SplitFilter byPrefix(@NonNull List<String> values) {
        return new SplitFilter(Type.BY_PREFIX, values);
    }

    public static SplitFilter bySet(@NonNull List<String> values) {
        if (values == null) {
            values = new ArrayList<>();
        }
        return new SplitFilter(Type.BY_SET, values, new FlagSetsValidatorImpl());
    }

    // This constructor is not private (but default) to allow Split Sync Config builder be agnostic when creating filters
    // Also is not public to force SDK users to use static functions "byName", "byPrefix", "bySet"
    SplitFilter(Type type, List<String> values) {
        if (values == null) {
            throw new IllegalArgumentException("Values can't be null for " + type.toString() + " filter");
        }
        mType = type;
        mValues = new ArrayList<>(values);
    }

    SplitFilter(Type type, List<String> values, SplitFilterValidator validator) {
        mType = type;
        SplitFilterValidator.ValidationResult validationResult = validator.cleanup("SDK config", values);
        mValues = validationResult.getValues();
        mInvalidValueCount = validationResult.getInvalidValueCount();
        mTotalValueCount = (values != null) ? values.size() - validationResult.getInvalidValueCount() : 0;
    }

    public Type getType() {
        return mType;
    }

    public List<String> getValues() {
        return mValues;
    }

    public int getInvalidValueCount() {
        return mInvalidValueCount;
    }

    public int getTotalValueCount() {
        return mTotalValueCount;
    }
}
