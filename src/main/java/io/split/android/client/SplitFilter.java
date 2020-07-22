package io.split.android.client;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SplitFilter {
    public enum Type {
        // Filters here has to be defined in the order
        // it will be in querystring
        BY_NAME,
        BY_PREFIX;

        @Override
        public String toString() {
            switch (this) {
                case BY_NAME:
                    return "by split name";
                case BY_PREFIX:
                    return "by split prefix";
                default:
                    return "Invalid type";
            }
        }
    }

    private final SplitFilter.Type type;
    private final Set<String> values;

    static public SplitFilter byName(@NonNull List<String> values) {
        return new SplitFilter(Type.BY_NAME, values);
    }

    static public SplitFilter byPrefix(@NonNull List<String> values) {
        return new SplitFilter(Type.BY_PREFIX, values);
    }

    private SplitFilter(Type type, List<String> values) {
        if (values == null) {
            throw new IllegalArgumentException("Values can't be null for " + type.toString() + " filter");
        }
        this.type = type;
        this.values = new HashSet<>(values);
    }

    public Type getType() {
        return type;
    }

    public Set<String> getValues() {
        return values;
    }

    public void deleteValue(String value) {
        values.remove(value);
    }
}
