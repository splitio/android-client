package io.split.android.client;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.utils.logger.Logger;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.SplitValidatorImpl;

public class SyncConfig {

    private final List<SplitFilter> mFilters;

    private final int mInvalidValueCount;
    private final int mTotalValueCount;

    private SyncConfig(List<SplitFilter> filters, int invalidValueCount, int totalValueCount) {
        mFilters = filters;
        mInvalidValueCount = invalidValueCount;
        mTotalValueCount = totalValueCount;
    }

    public List<SplitFilter> getFilters() {
        return mFilters;
    }

    public int getInvalidValueCount() {
        return mInvalidValueCount;
    }

    public int getTotalValueCount() {
        return mTotalValueCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<SplitFilter> mBuilderFilters = new ArrayList<>();
        private int mInvalidValueCount = 0;
        private int mTotalValueCount = 0;
        private final SplitValidator mSplitValidator = new SplitValidatorImpl();

        public SyncConfig build() {
            List<SplitFilter> validatedFilters = new ArrayList<>();
            for (SplitFilter filter : mBuilderFilters) {
                List<String> values = filter.getValues();
                List<String> validatedValues = new ArrayList<>();
                for (String value : values) {
                    if (mSplitValidator.validateName(value) != null) {
                        Logger.w(String.format("Warning: Malformed %s value. Filter ignored: %s", filter.getType().toString(), value));
                    } else {
                        validatedValues.add(value);
                    }
                }
                if (validatedValues.size() > 0) {
                    validatedFilters.add(new SplitFilter(filter.getType(), validatedValues));
                }
            }
            return new SyncConfig(validatedFilters, mInvalidValueCount, mTotalValueCount);
        }

        public Builder addSplitFilter(@NonNull SplitFilter filter) {
            if (filter == null) {
                throw new IllegalArgumentException("Filter can't be null");
            }
            mBuilderFilters.add(filter);
            mInvalidValueCount += filter.getInvalidValueCount();
            mTotalValueCount += filter.getTotalValueCount();
            return this;
        }
    }
}
