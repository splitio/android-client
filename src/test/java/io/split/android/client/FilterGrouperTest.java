package io.split.android.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FilterGrouperTest {

    FilterGrouper mFilterGrouper = new FilterGrouper();

    @Test
    public void groupingFilters() {
        List<SplitFilter> ungroupedFilters = new ArrayList<>();
        ungroupedFilters.add(SplitFilter.byName(Arrays.asList("f1", "f2", "f3")));
        ungroupedFilters.add(SplitFilter.byName(Arrays.asList("f2", "f3", "f4")));
        ungroupedFilters.add(SplitFilter.byName(Arrays.asList("f4", "f5", "f6")));
        ungroupedFilters.add(SplitFilter.byPrefix(Arrays.asList("f1", "f2", "f3")));
        ungroupedFilters.add(SplitFilter.byPrefix(Arrays.asList("f2", "f3", "f4")));
        ungroupedFilters.add(SplitFilter.byPrefix(Arrays.asList("f4", "f5", "f6")));
        ungroupedFilters.add(SplitFilter.bySet(Arrays.asList("f1", "f2", "f3")));
        ungroupedFilters.add(SplitFilter.bySet(Arrays.asList("f2", "f3", "f4")));
        ungroupedFilters.add(SplitFilter.bySet(Arrays.asList("f4", "f5", "f6")));

        Map<SplitFilter.Type, SplitFilter> groupedFilters = mFilterGrouper.group(ungroupedFilters);

        // this class only merges filters of the same type
        assertEquals(3, groupedFilters.size());
        assertTrue(groupedFilters.containsKey(SplitFilter.Type.BY_NAME));
        assertTrue(groupedFilters.containsKey(SplitFilter.Type.BY_PREFIX));
        assertTrue(groupedFilters.containsKey(SplitFilter.Type.BY_SET));
    }
}
