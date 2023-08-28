package io.split.android.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FilterGrouper {

    List<SplitFilter> group(List<SplitFilter> filters) {
        Map<SplitFilter.Type, List<String>> groupedValues = new HashMap<>();
        for (SplitFilter filter : filters) {
            List<String> groupValues = groupedValues.get(filter.getType());
            if (groupValues == null) {
                groupValues = new ArrayList<>();
                groupedValues.put(filter.getType(), groupValues);
            }
            groupValues.addAll(filter.getValues());
        }

        List<SplitFilter> groupedFilters = new ArrayList<>();
        for (Map.Entry<SplitFilter.Type, List<String>> filterEntry : groupedValues.entrySet()) {
            if (filterEntry.getValue().size() > 0) {
                groupedFilters.add(new SplitFilter(filterEntry.getKey(), filterEntry.getValue()));
            }
        }
        return groupedFilters;
    }
}
