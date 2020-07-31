package io.split.android.client;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterGrouperTest {

    FilterGrouper mFilterGrouper = new FilterGrouper();

    @Test
    public void groupingFilters() {
        List<SplitFilter> ungropedFilters = new ArrayList<>();
        ungropedFilters.add(SplitFilter.byName(Arrays.asList("f1", "f2", "f3")));
        ungropedFilters.add(SplitFilter.byName(Arrays.asList("f2", "f3", "f4")));
        ungropedFilters.add(SplitFilter.byName(Arrays.asList("f4", "f5", "f6")));
        ungropedFilters.add(SplitFilter.byPrefix(Arrays.asList("f1", "f2", "f3")));
        ungropedFilters.add(SplitFilter.byPrefix(Arrays.asList("f2", "f3", "f4")));
        ungropedFilters.add(SplitFilter.byPrefix(Arrays.asList("f4", "f5", "f6")));

        List<SplitFilter> groupedFiltes = mFilterGrouper.group(ungropedFilters);

        /// This compoe
        Assert.assertEquals(2, groupedFiltes.size());
    }

}
