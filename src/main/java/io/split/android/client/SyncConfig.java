package io.split.android.client;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.utils.Logger;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.SplitValidatorImpl;

import static androidx.core.util.Preconditions.checkNotNull;

public class SyncConfig {

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
            List<SplitFilter> filters = new ArrayList<>();
            for (SplitFilter filter : mBuilderFilters) {
                switch (filter.getType()) {
                    case BY_NAME:
                        if (filter.getValues().size() > MAX_BY_NAME_VALUES) {
                            String message = "Error: 400 different split names can be specified at most. You passed " +
                                    filter.getValues().size()
                                    + ". Please consider reducing the amount or using prefixes to target specific groups of splits.";
                            throw new IllegalArgumentException(message);
                        }
                        break;
                    case BY_PREFIX:
                        if (filter.getValues().size() > MAX_BY_PREFIX_VALUES) {
                            String message = "Error: 50 different prefixes can be specified at most. You passed %d." +
                                    filter.getValues().size() +
                                    "Please consider using a lower number of prefixes and/or filtering by split name as well.";

                            throw new IllegalArgumentException(message);
                        }
                        break;
                    default:
                        Logger.e("Invalid Split filter!");
                }

                List<String> valuesToValidate = new ArrayList<>(filter.getValues());
                for (String value : valuesToValidate) {
                    if (mSplitValidator.validateName(value) != null) {
                        Logger.w(String.format("Warning: Malformed %s value. Filter ignored: %s", filter.getType().toString(), value));
                        filter.deleteValue(value);
                    }
                }

                if (filter.getValues().size() > 0) {
                    filters.add(filter);
                }
            }
            return new SyncConfig(filters);
        }

        public Builder addSplitFilter(@NonNull SplitFilter filter) {
            if(filter == null) {
                throw new IllegalArgumentException("Filter can't be null");
            }
            mBuilderFilters.add(filter);
            return this;
        }
    }
}