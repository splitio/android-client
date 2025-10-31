package io.split.android.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FilterGrouper {

    /**
     * Groups filters by type
     * @param filters list of filters to group
     * @return map of grouped filters. The key is the filter type, the value is the filter
     */
    Map<SplitFilter.Type, SplitFilter> group(List<SplitFilter> filters) {
        Map<SplitFilter.Type, List<String>> groupedValues = new HashMap<>();
        for (SplitFilter filter : filters) {
            List<String> groupValues = groupedValues.get(filter.getType());
            if (groupValues == null) {
                groupValues = new ArrayList<>();
                groupedValues.put(filter.getType(), groupValues);
            }
            groupValues.addAll(filter.getValues());
        }

        Map<SplitFilter.Type, SplitFilter> groupedFilters = new HashMap<>();
        for (Map.Entry<SplitFilter.Type, List<String>> filterEntry : groupedValues.entrySet()) {
            if (filterEntry.getValue().size() > 0) {
                groupedFilters.put(filterEntry.getKey(), new SplitFilter(filterEntry.getKey(), filterEntry.getValue()));
            }
        }

        return groupedFilters;
    }
}
