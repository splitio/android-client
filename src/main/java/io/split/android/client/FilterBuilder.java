package io.split.android.client;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.StringHelper;

public class FilterBuilder {

    private final List<SplitFilter> mFilters = new ArrayList<>();
    private final FilterGrouper mFilterGrouper;

    public FilterBuilder(List<SplitFilter> filters) {
        this(new FilterGrouper(), filters);
    }

    FilterBuilder(@NonNull FilterGrouper filterGrouper, @Nullable List<SplitFilter> filters) {
        mFilterGrouper = checkNotNull(filterGrouper);
        addFilters(filters);
    }

    public String buildQueryString() {

        if (mFilters.size() == 0) {
            return "";
        }

        StringHelper stringHelper = new StringHelper();
        StringBuilder queryString = new StringBuilder();

        List<SplitFilter> sortedFilters = getGroupedFilter();
        Collections.sort(sortedFilters, new SplitFilterComparator());

        for (SplitFilter splitFilter : sortedFilters) {
            SplitFilter.Type filterType = splitFilter.getType();
            SortedSet<String> deduptedValues = new TreeSet<>(splitFilter.getValues());
            if (deduptedValues.size() < splitFilter.getValues().size()) {
                Logger.w("Warning: Some duplicated values for " + filterType.toString() + " filter  were removed.");
            }

            if (deduptedValues.size() == 0) {
                continue;
            }
            validateFilterSize(filterType, deduptedValues.size());

            queryString.append("&");
            queryString.append(filterType.queryStringField());
            queryString.append("=");
            queryString.append(stringHelper.join(",", deduptedValues));
        }

        return queryString.toString();
    }

    @NonNull
    public ArrayList<SplitFilter> getGroupedFilter() {
        return new ArrayList<>(mFilterGrouper.group(mFilters));
    }

    private void addFilters(List<SplitFilter> filters) {
        if (filters == null) {
            return;
        }

        boolean containsSetsFilter = false;
        for (SplitFilter filter : filters) {
            if (filter == null) {
                continue;
            }

            if (filter.getType() == SplitFilter.Type.BY_SET) {
                // BY_SET filter has precedence over other filters, so we remove all other filters
                // and only add BY_SET filters
                if (!containsSetsFilter) {
                    mFilters.clear();
                    containsSetsFilter = true;
                }
                mFilters.add(filter);
            }

            if (!containsSetsFilter) {
                mFilters.add(filter);
            }
        }

    }

    private void validateFilterSize(SplitFilter.Type type, int size) {
        if (size > type.maxValuesCount()) {
            String message = "Error: " + type.maxValuesCount() + " different split " + type.queryStringField() +
                    " can be specified at most. You passed " + size
                    + ". Please consider reducing the amount or using prefixes to target specific groups of splits.";
            throw new IllegalArgumentException(message);
        }
    }

    private static class SplitFilterComparator implements Comparator<SplitFilter> {
        @Override
        public int compare(SplitFilter o1, SplitFilter o2) {
            return o1.getType().compareTo(o2.getType());
        }
    }
}
