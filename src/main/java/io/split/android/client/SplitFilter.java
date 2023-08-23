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
        BY_SET,
        BY_PREFIX;

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
                    return 1000;
                default:
                    return 0;
            }
        }
    }

    private final SplitFilter.Type mType;
    private final List<String> mValues;

    static public SplitFilter byName(@NonNull List<String> values) {
        return new SplitFilter(Type.BY_NAME, values);
    }

    static public SplitFilter byPrefix(@NonNull List<String> values) {
        return new SplitFilter(Type.BY_PREFIX, values);
    }

    static public SplitFilter bySet(@NonNull List<String> values) {
        return new SplitFilter(Type.BY_SET, values, new FlagSetsValidatorImpl());
    }

    // This constructor is not private (but default) to allow Split Sync Config builder be agnostic when creating filters
    // Also is not public to force SDK users to use static functions "byName" and "byPrefix"
    SplitFilter(Type type, List<String> values) {
        if (values == null) {
            throw new IllegalArgumentException("Values can't be null for " + type.toString() + " filter");
        }
        mType = type;
        mValues = new ArrayList<>(values);
    }

    SplitFilter(Type type, List<String> values, SplitFilterValidator validator) {
        mType = type;
        mValues = validator.cleanup(values);
    }

    public Type getType() {
        return mType;
    }

    public List<String> getValues() {
        return mValues;
    }
}
