package io.split.android.client;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static androidx.core.util.Preconditions.checkNotNull;

public class SplitFilter {
    public enum Type {
        BY_NAME,
        BY_PREFIX;

        @Override
        public String toString() {
            switch(this) {
                case BY_NAME: return "by split name";
                case BY_PREFIX: return "by split prefix";
                default: return "Invalid mType";
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

    // This constructor is not private (but default) to allow Split Sync Config builder be agnostic when creating filters
    // Also is not public to force SDK users to use static functions "byName" and "byPrefix"
    SplitFilter(Type type, List<String> values) {
        if(values == null) {
            throw new IllegalArgumentException("Values can't be null for " + type.toString() + " filter");
        }
        mType = type;
        mValues = new ArrayList<>(values);
    }

    public Type getType() {
        return mType;
    }

    public List<String> getValues() {
        return mValues;
    }

    public void updateValues(List<String> values) {
        mValues.clear();
        mValues.addAll(values);
    }
}
