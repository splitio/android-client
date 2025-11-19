package io.split.android.client.service.workmanager.splits;

import androidx.annotation.Nullable;

import io.split.android.client.FlagSetsFilter;
import io.split.android.client.FlagSetsFilterImpl;
import io.split.android.client.SplitFilter;
import io.split.android.client.service.splits.SplitChangeProcessor;

class SplitChangeProcessorProvider {

    SplitChangeProcessor provideSplitChangeProcessor(String filterType, String[] filterValues) {
        @Nullable SplitFilter filter = SplitsSyncWorkerFilterBuilder.buildFilter(filterType, filterValues);
        FlagSetsFilter flagSetsFilter = null;
        if (filter != null && filter.getType() == SplitFilter.Type.BY_SET) {
            flagSetsFilter = new FlagSetsFilterImpl(filter.getValues());
        }

        return new SplitChangeProcessor(filter, flagSetsFilter);
    }
}
