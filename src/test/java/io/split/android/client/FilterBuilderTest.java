package io.split.android.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FilterBuilderTest {

    @Test
    public void testBasicQueryString() {
        // Test that builder generates a query string having the byName filter first
        // then byPrefix filter. Also values should be ordered in each filter
        SplitFilter byNameFilter = SplitFilter.byName(Arrays.asList("nf_a", "nf_c", "nf_b"));
        SplitFilter byPrefixFilter = SplitFilter.byPrefix(Arrays.asList("pf_c", "pf_b", "pf_a"));

        String queryString = new FilterBuilder().addFilters(Arrays.asList(byNameFilter, byPrefixFilter)).buildQueryString();

        assertEquals("&names=nf_a,nf_b,nf_c&prefixes=pf_a,pf_b,pf_c", queryString);
    }

    @Test
    public void testOnlyOneTypeQueryString() {
        // When one filter is not present, it has to appear as empty in querystring
        // fields order has to be maintained in querystring
        SplitFilter byNameFilter = SplitFilter.byName(Arrays.asList("nf_a", "nf_c", "nf_b"));
        SplitFilter byPrefixFilter = SplitFilter.byPrefix(Arrays.asList("pf_c", "pf_b", "pf_a"));

        String onlyByNameQs = new FilterBuilder().addFilters(Arrays.asList(byNameFilter)).buildQueryString();
        String onlyByPrefixQs = new FilterBuilder().addFilters(Arrays.asList(byPrefixFilter)).buildQueryString();

        assertEquals("&names=nf_a,nf_b,nf_c", onlyByNameQs);
        assertEquals("&prefixes=pf_a,pf_b,pf_c", onlyByPrefixQs);
    }

    @Test
    public void filterValuesDeduptedAndGrouped() {
        // Duplicated filter values should be removed on building

        List<SplitFilter> filters = Arrays.asList(
                SplitFilter.byName(Arrays.asList("nf_a", "nf_c", "nf_b")),
                SplitFilter.byName(Arrays.asList("nf_b", "nf_d")),
                SplitFilter.byPrefix(Arrays.asList("pf_a", "pf_c", "pf_b")),
                SplitFilter.byPrefix(Arrays.asList("pf_d", "pf_a")));

        String queryString = new FilterBuilder()
                .addFilters(filters)
                .buildQueryString();

        assertEquals("&names=nf_a,nf_b,nf_c,nf_d&prefixes=pf_a,pf_b,pf_c,pf_d", queryString);
    }

    @Test
    public void maxByNameFilterExceded() {
        // More values than 400 should cause InvalidArgumentException

        boolean exceptionThrown = false;
        List<String> values = new ArrayList<>();
        for (int i = 0; i < 401; i++) {
            values.add("filter" + i);
        }

        try {
            String queryString = new FilterBuilder()
                    .addFilters(Arrays.asList(SplitFilter.byName(values)))
                    .buildQueryString();
        } catch (Exception e) {
            exceptionThrown = true;
        }

        Assert.assertTrue(exceptionThrown);
    }

    @Test
    public void maxByPrefixFilterExceded() {
        // More values than 400 should cause InvalidArgumentException

        boolean exceptionThrown = false;
        List<String> values = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            values.add("filter" + i);
        }

        try {
            String queryString = new FilterBuilder()
                    .addFilters(Arrays.asList(SplitFilter.byPrefix(values)))
                    .buildQueryString();
        } catch (Exception e) {
            exceptionThrown = true;
        }

        Assert.assertTrue(exceptionThrown);
    }

    @Test
    public void testNoFilters() {
        // When no filter added, query string has to be empty

        String queryString = new FilterBuilder().buildQueryString();

        assertEquals("", queryString);
    }

    @Test
    public void testQueryStringWithSpecialChars1() {
        SyncConfig config = SyncConfig.builder()
                .addSplitFilter(SplitFilter.byName(Arrays.asList("\u0223abc", "abc\u0223asd", "abc\u0223")))
                .addSplitFilter(SplitFilter.byName(Arrays.asList("ausgefüllt")))
                .addSplitFilter(SplitFilter.byPrefix(Arrays.asList()))
                .build();
        String queryString = new FilterBuilder().addFilters(config.getFilters()).buildQueryString();
        assertEquals("&names=abc\u0223,abc\u0223asd,ausgefüllt,\u0223abc", queryString);
    }

    @Test
    public void testQueryStringWithSpecialChars2() {
        SyncConfig config = SyncConfig.builder()
                .addSplitFilter(SplitFilter.byPrefix(Arrays.asList("\u0223abc", "abc\u0223asd", "abc\u0223")))
                .addSplitFilter(SplitFilter.byPrefix(Arrays.asList("ausgefüllt")))
                .addSplitFilter(SplitFilter.byName(Arrays.asList()))
                .build();
        String queryString = new FilterBuilder().addFilters(config.getFilters()).buildQueryString();
        assertEquals("&prefixes=abc\u0223,abc\u0223asd,ausgefüllt,\u0223abc", queryString);
    }
    
    @Test
    public void testQueryStringWithSpecialChars3() {
        SyncConfig config = SyncConfig.builder()
                .addSplitFilter(SplitFilter.byName(Arrays.asList("\u0223abc", "abc\u0223asd", "abc\u0223")))
                .addSplitFilter(SplitFilter.byName(Arrays.asList("ausgefüllt")))
                .addSplitFilter(SplitFilter.byPrefix(Arrays.asList("\u0223abc", "abc\u0223asd", "abc\u0223")))
                .addSplitFilter(SplitFilter.byPrefix(Arrays.asList("ausgefüllt")))
                .build();
        String queryString = new FilterBuilder().addFilters(config.getFilters()).buildQueryString();
        assertEquals("&names=abc\u0223,abc\u0223asd,ausgefüllt,\u0223abc&prefixes=abc\u0223,abc\u0223asd,ausgefüllt,\u0223abc", queryString);
    }

    @Test
    public void testQueryStringWithSpecialChars4() {
        SyncConfig config = SyncConfig.builder()
                .addSplitFilter(SplitFilter.byName(Arrays.asList("__ш", "__a", "%", "%25", " __ш ", "%  ")))
                .build();
        String queryString = new FilterBuilder().addFilters(config.getFilters()).buildQueryString();
        assertEquals("&names=%,%25,__a,__ш", queryString);
    }

    @Test
    public void addingBySetFilterAlongsideOtherTypesLeavesOnlyBySet() {
        List<SplitFilter> filters = Arrays.asList(
                SplitFilter.byName(Arrays.asList("nf_a", "nf_c", "nf_b")),
                SplitFilter.byName(Arrays.asList("nf_b", "nf_d")),
                SplitFilter.bySet(Collections.singletonList("zz")),
                SplitFilter.byPrefix(Arrays.asList("pf_a", "pf_c", "pf_b")),
                SplitFilter.bySet(Arrays.asList("pf_d", "pf_a", "_invalid")));

        String queryString = new FilterBuilder().addFilters(filters).buildQueryString();

        assertEquals("&sets=pf_a,pf_d,zz", queryString);
    }

    @Test
    public void bySetQueryStringIsBuiltCorrectly() {
        String queryString = new FilterBuilder().addFilters(Arrays.asList(SplitFilter.bySet(Arrays.asList("pf_d", "pf_a", "_invalid")))).buildQueryString();

        assertEquals("&sets=pf_a,pf_d", queryString);
    }

    @Test
    public void addingMultipleBySetFiltersCombinesTheValues() {
        List<SplitFilter> filters = Arrays.asList(
                SplitFilter.bySet(Arrays.asList("pf_d", "pf_a", "_invalid")),
                SplitFilter.bySet(Arrays.asList("pf_d", "pf_c", "_invalid")),
                SplitFilter.bySet(Arrays.asList("zz", "zzz")));

        String queryString = new FilterBuilder().addFilters(filters).buildQueryString();

        assertEquals("&sets=pf_a,pf_c,pf_d,zz,zzz", queryString);
    }

    @Test
    public void getGroupedFiltersUsesFilterGrouper() {
        FilterGrouper filterGrouper = mock(FilterGrouper.class);
        FilterBuilder filterBuilder = new FilterBuilder(filterGrouper).addFilters(Collections.emptyList());

        filterBuilder.getGroupedFilter();

        verify(filterGrouper).group(Collections.emptyList());
    }
}
