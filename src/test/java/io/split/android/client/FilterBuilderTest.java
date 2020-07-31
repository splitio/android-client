package io.split.android.client;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterBuilderTest {

    @Test
    public void testBasicQueryString() {
        // Test that builder generates a query string having the byName filter first
        // then byPrefix filter. Also values should be ordered in each filter
        SplitFilter byNameFilter = SplitFilter.byName(Arrays.asList("nf_a", "nf_c", "nf_b"));
        SplitFilter byPrefixFilter = SplitFilter.byPrefix(Arrays.asList("pf_c", "pf_b", "pf_a"));

        String queryString = new FilterBuilder().addFilters(Arrays.asList(byNameFilter, byPrefixFilter)).build();

        Assert.assertEquals("&names=nf_a,nf_b,nf_c&prefixes=pf_a,pf_b,pf_c", queryString);
    }

    @Test
    public void testOnlyOneTypeQueryString() {
        // When one filter is not present, it has to appear as empty in querystring
        // fields order has to be maintained in querystring
        SplitFilter byNameFilter = SplitFilter.byName(Arrays.asList("nf_a", "nf_c", "nf_b"));
        SplitFilter byPrefixFilter = SplitFilter.byPrefix(Arrays.asList("pf_c", "pf_b", "pf_a"));

        String onlyByNameQs = new FilterBuilder().addFilters(Arrays.asList(byNameFilter)).build();
        String onlyByPrefixQs = new FilterBuilder().addFilters(Arrays.asList(byPrefixFilter)).build();

        Assert.assertEquals("&names=nf_a,nf_b,nf_c", onlyByNameQs);
        Assert.assertEquals("&prefixes=pf_a,pf_b,pf_c", onlyByPrefixQs);
    }

    @Test
    public void filterValuesDeduptedAndGrouped() {
        // Duplicated filter values should be removed on builing

        List<SplitFilter> filters = Arrays.asList(
                SplitFilter.byName(Arrays.asList("nf_a", "nf_c", "nf_b")),
                SplitFilter.byName(Arrays.asList("nf_b", "nf_d")),
                SplitFilter.byPrefix(Arrays.asList("pf_a", "pf_c", "pf_b")),
                SplitFilter.byPrefix(Arrays.asList("pf_d", "pf_a")));

        String queryString = new FilterBuilder()
                .addFilters(filters)
                .build();

        Assert.assertEquals("&names=nf_a,nf_b,nf_c,nf_d&prefixes=pf_a,pf_b,pf_c,pf_d", queryString);
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
                    .build();
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
                    .build();
        } catch (Exception e) {
            exceptionThrown = true;
        }

        Assert.assertTrue(exceptionThrown);
    }

    @Test
    public void testNoFilters() {
        // When no filter added, query string has to be empty

        String queryString = new FilterBuilder().build();

        Assert.assertEquals("", queryString);
    }
}
