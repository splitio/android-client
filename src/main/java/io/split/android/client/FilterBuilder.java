package io.split.android.client;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import io.split.android.client.utils.logger.Logger;

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
        if (mFilters.isEmpty()) {
            return "";
        }

        StringBuilder queryString = new StringBuilder();

        Map<SplitFilter.Type, SplitFilter> sortedFilters = getGroupedFilter();

        for (SplitFilter splitFilter : sortedFilters.values()) {
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
            queryString.append(String.join(",", deduptedValues));
        }

        return queryString.toString();
    }

    @NonNull
    public Map<SplitFilter.Type, SplitFilter> getGroupedFilter() {
        TreeMap<SplitFilter.Type, SplitFilter> sortedFilters = new TreeMap<>(new SplitFilterTypeComparator());
        sortedFilters.putAll(mFilterGrouper.group(mFilters));

        return sortedFilters;
    }

    private void addFilters(List<SplitFilter> filters) {
        if (filters == null) {
            return;
        }

        Set<SplitFilter.Type> presentTypes = new HashSet<>();
        boolean containsSetsFilter = false;
        for (SplitFilter filter : filters) {
            if (filter == null) {
                continue;
            }

            presentTypes.add(filter.getType());
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

        if (presentTypes.contains(SplitFilter.Type.BY_SET) && presentTypes.size() > 1) {
            Logger.e("SDK Config: The Set filter is exclusive and cannot be used simultaneously with names or prefix filters. Ignoring names and prefixes");
        }
    }

    private void validateFilterSize(SplitFilter.Type type, int size) {
        if (size > type.maxValuesCount()) {
            String message = "Error: " + type.maxValuesCount() + " different feature flag " + type.queryStringField() +
                    " can be specified at most. You passed " + size
                    + ". Please consider reducing the amount or using prefixes to target specific groups of feature flags.";
            throw new IllegalArgumentException(message);
        }
    }

    private static class SplitFilterTypeComparator implements Comparator<SplitFilter.Type> {
        @Override
        public int compare(SplitFilter.Type o1, SplitFilter.Type o2) {
            return o1.compareTo(o2);
        }
    }
}
