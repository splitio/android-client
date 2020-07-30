package io.split.android.client;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SyncConfigTest {
    @Before
    public void setup() {
    }

    @Test
    public void testFilterByName() {
        // Testing basic by name filter creation
        // and checking correctnes in values
        SplitFilter filter = SplitFilter.byName(Arrays.asList("f1", "f2", "f3"));

        Assert.assertEquals(SplitFilter.Type.BY_NAME, filter.getType());
        Assert.assertTrue(filter.getValues().contains("f1"));
        Assert.assertTrue(filter.getValues().contains("f2"));
        Assert.assertTrue(filter.getValues().contains("f3"));
    }

    @Test
    public void testFilterByPrefix() {
        // Testing basic by prefix filter creation
        // and checking correctnes in values
        SplitFilter filter = SplitFilter.byPrefix(Arrays.asList("f1", "f2", "f3"));

        Assert.assertEquals(SplitFilter.Type.BY_PREFIX, filter.getType());
        Assert.assertTrue(filter.getValues().contains("f1"));
        Assert.assertTrue(filter.getValues().contains("f2"));
        Assert.assertTrue(filter.getValues().contains("f3"));
    }

    @Test
    public void addingNullValueList() {
        // Null value list is not valid.
        // Split filter should throw invalid argument exception
        SplitFilter filterByName = null;
        SplitFilter filterByPrefix = null;
        boolean byNameExceptionThrown = false;
        boolean byPrefixExceptionThrown = false;
        try {
            filterByName = SplitFilter.byName(null);
        } catch (Exception e) {
            byNameExceptionThrown = true;
        }

        try {
            filterByPrefix = SplitFilter.byPrefix(null);
        } catch (Exception e) {
            byPrefixExceptionThrown = true;
        }
        Assert.assertTrue(byNameExceptionThrown);
        Assert.assertTrue(byPrefixExceptionThrown);
        Assert.assertNull(filterByName);
        Assert.assertNull(filterByPrefix);
    }

    @Test
    public void testSyncBuilder() {
        // Testing basic sync config creation
        // by creating a filters having max allowed values

        List<String> byPrefixValues = new ArrayList<String>();
        for (int i = 1; i <= 40; i++) {
            byPrefixValues.add("f" + i);
        }
        SplitFilter byPrefixFilter = SplitFilter.byPrefix(byPrefixValues);

        List<String> byNameValues = new ArrayList<String>();
        for (int i = 1; i <= 400; i++) {
            byNameValues.add("f" + i);
        }
        SplitFilter byNameFilter = SplitFilter.byName(byNameValues);

        SyncConfig config = SyncConfig.builder().addSplitFilter(byNameFilter).addSplitFilter(byPrefixFilter).build();

        SplitFilter byNameAdded = null;
        SplitFilter byPrefixAdded = null;

        List<SplitFilter> filters = config.getFilters();
        for (SplitFilter filter : filters) {
            if (SplitFilter.Type.BY_NAME.equals(filter.getType())) {
                byNameAdded = filter;
            } else {
                byPrefixAdded = filter;
            }
        }

        Assert.assertEquals(2, filters.size());
        Assert.assertNotNull(byNameAdded);
        Assert.assertNotNull(byPrefixAdded);
    }

    @Test
    public void testInvalidFilterValuesDiscarded() {
        // Filters that doesn't pass split rules
        // has to be removed from the list
        // This test adds some invalid ones an thes correct deletion
        SplitFilter byName = SplitFilter.byName(Arrays.asList("", "f2", null));
        SplitFilter byPrefix = SplitFilter.byPrefix(Arrays.asList("", "f2", null));

        SyncConfig config = SyncConfig.builder().addSplitFilter(byName).addSplitFilter(byPrefix).build();

        SplitFilter byNameAdded = null;
        SplitFilter byPrefixAdded = null;
        List<SplitFilter> filters = config.getFilters();
        for (SplitFilter filter : filters) {
            if (SplitFilter.Type.BY_NAME.equals(filter.getType())) {
                byNameAdded = filter;
            } else {
                byPrefixAdded = filter;
            }
        }

        Assert.assertEquals(1, byNameAdded.getValues().size());
        Assert.assertEquals(1, byPrefixAdded.getValues().size());
    }

    @Test
    public void emptyFilterValuesDiscarded() {
        // Empty lists should be discarded
        // Here we create two filters:
        // By name having no values and by prefix having all invalid values
        // No tests has to be added to SyncConfig

        SplitFilter byName = SplitFilter.byName(Arrays.asList());
        SplitFilter byPrefix = SplitFilter.byPrefix(Arrays.asList("", null));

        SyncConfig config = SyncConfig.builder().addSplitFilter(byName).addSplitFilter(byPrefix).build();

        SplitFilter byNameAdded = null;
        SplitFilter byPrefixAdded = null;
        List<SplitFilter> filters = config.getFilters();
        for (SplitFilter filter : filters) {
            if (SplitFilter.Type.BY_NAME.equals(filter.getType())) {
                byNameAdded = filter;
            } else {
                byPrefixAdded = filter;
            }
        }

        Assert.assertEquals(0, config.getFilters().size());
    }

    @Test
    public void addingNullFilterToConfig() {
        // Null filter is not valid.
        // Sync config builder should throw invalid argument exception

        boolean exceptionThrown = false;
        SyncConfig config = null;
        try {
            config = SyncConfig.builder().addSplitFilter(null).build();
        } catch (Exception e) {
            exceptionThrown = true;
        }

        Assert.assertTrue(exceptionThrown);
        Assert.assertNull(config);
    }
}
