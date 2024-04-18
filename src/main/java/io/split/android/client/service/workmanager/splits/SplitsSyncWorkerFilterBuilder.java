package io.split.android.client.service.workmanager.splits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.split.android.client.SplitFilter;

class SplitsSyncWorkerFilterBuilder {

    static SplitFilter buildFilter(String filterType, String[] filterValuesArray) {
        SplitFilter filter = null;
        if (filterType != null) {
            List<String> configuredFilterValues = new ArrayList<>();
            if (filterValuesArray != null) {
                configuredFilterValues = Arrays.asList(filterValuesArray);
            }

            if (SplitFilter.Type.BY_NAME.queryStringField().equals(filterType)) {
                filter = SplitFilter.byName(configuredFilterValues);
            } else if (SplitFilter.Type.BY_SET.queryStringField().equals(filterType)) {
                filter = SplitFilter.bySet(configuredFilterValues);
            }
        }
        return filter;
    }
}
