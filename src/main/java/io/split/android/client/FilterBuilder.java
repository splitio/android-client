package io.split.android.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.utils.Logger;
import io.split.android.client.utils.StringHelper;

public class FilterBuilder {
    private final List<SplitFilter> filters = new ArrayList<>();
    private final static String QUERYSTRING_TEMPLATE = "names=[by_name_filters]&prefixes=[by_prefix_filters]";

    static private class SplitFilterComparator implements Comparator<SplitFilter> {
        @Override
        public int compare(SplitFilter o1, SplitFilter o2) {
            return o1.getType().compareTo(o2.getType());
        }
    }

    public FilterBuilder addFilter(SplitFilter filter) {
        filters.add(filter);
        return this;
    }

    public String build() {

        if(filters.size() == 0) {
            return "";
        }

        StringHelper stringHelper = new StringHelper();
        StringBuilder queryString = new StringBuilder("");
        Map<SplitFilter.Type, List<String>> allFilters = new HashMap<>();

        List<SplitFilter> sortedFilters = new ArrayList(filters);
        Collections.sort(sortedFilters, new SplitFilterComparator());

        // Grouping filters
        for (SplitFilter splitFilter : sortedFilters) {
            List<String> values = allFilters.get(splitFilter.getType());
            if (values == null) {
                values = new ArrayList<>();
            }
            values.addAll(splitFilter.getValues());
            allFilters.put(splitFilter.getType(), values);
        }

        List<SplitFilter.Type> filterTypes = new ArrayList(allFilters.keySet());
        // This step sorts based on enum wich is built to
        Collections.sort(filterTypes);

        for (SplitFilter.Type filterType : filterTypes) {
            List<String> values = allFilters.get(filterType);
            Set<String> deduptedValues = new HashSet<>(values);
            if(deduptedValues.size() < values.size()) {
                Logger.w("Warning: Some duplicated values for " + filterType.toString() + " filter  were removed.");
            }

            // Creating array list to sort values
            values = new ArrayList<>(deduptedValues);
            Collections.sort(values);
            queryString.append(fieldNameByType(filterType));
            queryString.append("=");
            queryString.append(stringHelper.join(",", values));
            queryString.append("&");
        }
        return queryString.deleteCharAt(queryString.length() - 1).toString();
    }

    private String fieldNameByType(SplitFilter.Type type) {
        switch (type) {
            case BY_NAME:
                return "names";
            case BY_PREFIX:
                return "prefixes";
            default:
                Logger.e("Unknown filter: " + type.toString());
        }
        return "unknown";
    }

}
