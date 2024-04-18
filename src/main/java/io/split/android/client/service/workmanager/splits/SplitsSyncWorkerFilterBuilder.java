package io.split.android.client.service.workmanager.splits;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.split.android.client.SplitFilter;

class SplitsSyncWorkerFilterBuilder {

    @Nullable
    static SplitFilter buildFilter(@Nullable String filterType, String[] filterValues) {
        SplitFilter filter = null;
        if (filterType != null) {
            List<String> configuredFilterValues = new ArrayList<>();
            if (filterValues != null) {
                configuredFilterValues = Arrays.asList(filterValues);
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
