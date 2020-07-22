package io.split.android.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.split.android.client.utils.Logger;
import io.split.android.client.utils.StringHelper;

public class FilterBuilder {
    private final List<SplitFilter> filters = new ArrayList<>();

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
        StringHelper stringHelper = new StringHelper();
        StringBuilder queryString = new StringBuilder("");

        List<SplitFilter> sortedFilters = new ArrayList(filters);
        Collections.sort(sortedFilters, new SplitFilterComparator());

        for (SplitFilter filter : sortedFilters) {
            List<String> values = new ArrayList(filter.getValues());
            Collections.sort(values);
            queryString.append(fieldNameByType(filter.getType()));
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
