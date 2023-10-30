package io.split.android.client.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import io.split.android.client.SplitFilter;
import io.split.android.client.dtos.Split;
import io.split.android.client.service.splits.FilterSplitsInCacheTask;
import io.split.android.client.storage.splits.PersistentSplitsStorage;

public class FilterSplitsInCacheTaskTest {

    @Mock
    PersistentSplitsStorage mSplitsStorage;

    FilterSplitsInCacheTask mTask;
    List<SplitFilter> mFilters;

    private AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        mFilters = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void changedQueryStringAndKeepNames() {
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Split split = new Split();
            split.name = "sp" + i;
            splits.add(split);
        }

        mFilters.add(SplitFilter.byName(Arrays.asList("sp1", "sp2", "sp3")));
        when(mSplitsStorage.getFilterQueryString()).thenReturn("names=s1");
        when(mSplitsStorage.getAll()).thenReturn(splits);
        mTask = new FilterSplitsInCacheTask(mSplitsStorage, mFilters, "names=sp1,sp2,sp3");
        mTask.execute();

        verify(mSplitsStorage, times(1)).delete(Arrays.asList("sp0", "sp4"));
    }

    @Test
    public void changedQueryStringAndKeepPrefixes() {
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Split split = new Split();
            split.name = "sp" + i + "__split";
            splits.add(split);
        }

        mFilters.add(SplitFilter.byPrefix(Arrays.asList("sp1", "sp2", "sp3")));
        when(mSplitsStorage.getFilterQueryString()).thenReturn("names=s1");
        when(mSplitsStorage.getAll()).thenReturn(splits);
        mTask = new FilterSplitsInCacheTask(mSplitsStorage, mFilters, "names=sp1,sp2,sp3");
        mTask.execute();

        verify(mSplitsStorage, times(1)).delete(Arrays.asList("sp0__split", "sp4__split"));
    }

    @Test
    public void changedQueryStringAndKeepBoth() {
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Split split = new Split();
            split.name = "sp" + i + "__split";
            splits.add(split);
            split = new Split();
            split.name = "sp" + i;
            splits.add(split);
        }

        mFilters.add(SplitFilter.byName(Arrays.asList("sp1", "sp2", "sp3")));
        mFilters.add(SplitFilter.byPrefix(Arrays.asList("sp1", "sp2", "sp3")));
        when(mSplitsStorage.getFilterQueryString()).thenReturn("names=s1");
        when(mSplitsStorage.getAll()).thenReturn(splits);
        mTask = new FilterSplitsInCacheTask(mSplitsStorage, mFilters, "names=sp1,sp2,sp3");
        mTask.execute();

        verify(mSplitsStorage, times(1)).delete(Arrays.asList("sp0__split", "sp0", "sp4__split", "sp4"));
    }

    @Test
    public void noChangedQueryString() {
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Split split = new Split();
            split.name = "sp" + i + "__split";
            splits.add(split);
        }

        mFilters.add(SplitFilter.byName(Arrays.asList("sp1", "sp2", "sp3")));
        mFilters.add(SplitFilter.byPrefix(Arrays.asList("sp1", "sp2", "sp3")));
        when(mSplitsStorage.getFilterQueryString()).thenReturn("names=sp1,sp2,sp3");
        when(mSplitsStorage.getAll()).thenReturn(splits);
        mTask = new FilterSplitsInCacheTask(mSplitsStorage, mFilters, "names=sp1,sp2,sp3");
        mTask.execute();

        verify(mSplitsStorage, never()).delete(any());
    }

    @Test
    public void changedQueryStringNoSplitsToDelete() {
        List<Split> splits = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            Split split = new Split();
            split.name = "sp" + i;
            splits.add(split);
        }

        mFilters.add(SplitFilter.byName(Arrays.asList("sp1", "sp2", "sp3", "sp4")));
        when(mSplitsStorage.getFilterQueryString()).thenReturn("names=sp1,sp2,sp3");
        when(mSplitsStorage.getAll()).thenReturn(splits);
        mTask = new FilterSplitsInCacheTask(mSplitsStorage, mFilters, "names=sp1,sp2,sp3,sp4");
        mTask.execute();

        verify(mSplitsStorage, never()).delete(any());
    }

    @Test
    public void deleteSplitsNotInSet() {
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Split split = new Split();
            split.name = "sp" + i;
            split.sets = new HashSet<>();

            if (i % 3 == 0) {
                split.sets.add("set1");
            }
            splits.add(split);
        }

        mFilters.add(SplitFilter.bySet(Collections.singletonList("set1")));
        when(mSplitsStorage.getFilterQueryString()).thenReturn("sets=set2");
        when(mSplitsStorage.getAll()).thenReturn(splits);
        mTask = new FilterSplitsInCacheTask(mSplitsStorage, mFilters, "sets=set1");
        mTask.execute();

        assertEquals(5, splits.size());
        verify(mSplitsStorage).delete(Arrays.asList("sp1", "sp2", "sp4"));
    }

    @Test
    public void changedSetsQueryNoSplitsToDelete() {
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Split split = new Split();
            split.name = "sp" + i;
            split.sets = new HashSet<>();

            if (i % 3 == 0) {
                split.sets.add("set1");
            } else {
                split.sets.add("set2");
            }
            splits.add(split);
        }

        mFilters.add(SplitFilter.bySet(Arrays.asList("set1", "set2")));
        when(mSplitsStorage.getFilterQueryString()).thenReturn("sets=set1,set2");
        when(mSplitsStorage.getAll()).thenReturn(splits);
        mTask = new FilterSplitsInCacheTask(mSplitsStorage, mFilters, "sets=set1");
        mTask.execute();

        assertEquals(5, splits.size());
        verify(mSplitsStorage, never()).delete(any());
    }
}
