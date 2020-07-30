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

import static io.split.android.client.SplitFilter.Type.BY_NAME;
import static io.split.android.client.SplitFilter.Type.BY_PREFIX;

public class FilterBuilder {

    private final static int MAX_BY_NAME_VALUES = 400;
    private final static int MAX_BY_PREFIX_VALUES = 50;

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

        List<SplitFilter> sortedFilters = new ArrayList(filters);
        Collections.sort(sortedFilters, new SplitFilterComparator());

        for (SplitFilter splitFilter : sortedFilters) {
            SplitFilter.Type filterType = splitFilter.getType();
            Set<String> deduptedValues = new HashSet<>(splitFilter.getValues());
            if(deduptedValues.size() < splitFilter.getValues().size()) {
                Logger.w("Warning: Some duplicated values for " + filterType.toString() + " filter  were removed.");
            }

            if(deduptedValues.size() == 0) {
                continue;
            }
            validateFilterSize(filterType, deduptedValues.size());

            // Creating array list to sort values
            List<String> values = new ArrayList<>(deduptedValues);
            Collections.sort(values);
            queryString.append("&");
            queryString.append(fieldNameByType(filterType));
            queryString.append("=");
            queryString.append(stringHelper.join(",", values));
        }
        return queryString.toString();
    }

    private void validateFilterSize(SplitFilter.Type type, int size) {
        switch (type) {
            case BY_NAME:
                if (size > MAX_BY_NAME_VALUES) {
                    String message = "Error: 400 different split names can be specified at most. You passed " + size
                            + ". Please consider reducing the amount or using prefixes to target specific groups of splits.";
                    throw new IllegalArgumentException(message);
                }
                break;
            case BY_PREFIX:
                if (size > MAX_BY_PREFIX_VALUES) {
                    String message = "Error: 50 different prefixes can be specified at most. You passed %d." + size +
                            "Please consider using a lower number of prefixes and/or filtering by split name as well.";

                    throw new IllegalArgumentException(message);
                }
                break;
            default:
                Logger.e("Invalid Split filter!");
        }
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
