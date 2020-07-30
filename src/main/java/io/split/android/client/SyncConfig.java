package io.split.android.client;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.utils.Logger;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.SplitValidatorImpl;

class SyncConfig {

    private final static int MAX_BY_NAME_VALUES = 400;
    private final static int MAX_BY_PREFIX_VALUES = 50;
    private final List<SplitFilter> mFilters;


    private SyncConfig(List<SplitFilter> filters) {
        mFilters = filters;
    }

    public List<SplitFilter> getFilters() {
        return mFilters;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<SplitFilter> mBuilderFilters = new ArrayList<>();
        private final SplitValidator mSplitValidator = new SplitValidatorImpl();

        public SyncConfig build() {
            Map<SplitFilter.Type, List<String>> groupedFilters = new HashMap<>();
            for (SplitFilter filter : mBuilderFilters) {
                List<String> groupedValues = groupedFilters.get(filter.getType());
                if (groupedValues == null) {
                    groupedValues = new ArrayList<>();
                    groupedFilters.put(filter.getType(), groupedValues);
                }

                List<String> values = filter.getValues();
                for (String value : values) {
                    if (mSplitValidator.validateName(value) != null) {
                        Logger.w(String.format("Warning: Malformed %s value. Filter ignored: %s", filter.getType().toString(), value));
                    } else {
                        groupedValues.add(value);
                    }
                }
            }

            List<SplitFilter> filters = new ArrayList<>();
            for (Map.Entry<SplitFilter.Type, List<String>> filterEntry : groupedFilters.entrySet()) {
                if (filterEntry.getValue().size() > 0) {
                    filters.add(new SplitFilter(filterEntry.getKey(), filterEntry.getValue()));
                }
            }
            return new SyncConfig(filters);
        }

        public Builder addSplitFilter(@NonNull SplitFilter filter) {
            if (filter == null) {
                throw new IllegalArgumentException("Filter can't be null");
            }
            mBuilderFilters.add(filter);
            return this;
        }
    }
}