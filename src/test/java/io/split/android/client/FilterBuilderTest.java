package io.split.android.client;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.split.android.client.utils.Logger;

public class FilterBuilderTest {

    @Test
    public void testBasicQueryString() {
        // Test that builder generates a query string having the byName filter first
        // then byPrefix filter. Also values should be ordered in each filter
        SplitFilter byNameFilter = SplitFilter.byName(Arrays.asList("nf_a", "nf_c", "nf_b"));
        SplitFilter byPrefixFilter = SplitFilter.byPrefix(Arrays.asList("pf_c", "pf_b", "pf_a"));

        String queryString = new FilterBuilder().addFilter(byNameFilter).addFilter(byPrefixFilter).build();

        Assert.assertEquals("names=nf_a,nf_b,nf_c&prefixes=pf_a,pf_b,pf_c", queryString);
    }

    @Test
    public void testOnlyByNameQueryString() {
        // When one filter is not present, it has to appear as empty in querystring
        // fields order has to be maintained in querystring
        SplitFilter byNameFilter = SplitFilter.byName(Arrays.asList("nf_a", "nf_c", "nf_b"));
        SplitFilter byPrefixFilter = SplitFilter.byPrefix(Arrays.asList("pf_c", "pf_b", "pf_a"));

        String onlyByNameQs = new FilterBuilder().addFilter(byNameFilter).build();
        String onlyByPrefixQs = new FilterBuilder().addFilter(byPrefixFilter).build();

        Assert.assertEquals("names=nf_a,nf_b,nf_c&prefixes=", onlyByNameQs);
        Assert.assertEquals("names=nf_a,nf_b,nf_c&prefixes=pf_a,pf_b,pf_c", onlyByPrefixQs);
    }
}
