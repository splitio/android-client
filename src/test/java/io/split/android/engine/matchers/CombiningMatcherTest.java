package io.split.android.engine.matchers;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import io.split.android.client.dtos.MatcherCombiner;
import io.split.android.engine.matchers.strings.WhitelistMatcher;

/**
 * Tests CombiningMatcher
 *
 */
public class CombiningMatcherTest {

    @Test
    public void works_and() {
        AttributeMatcher matcher1 = AttributeMatcher.vanilla(new AllKeysMatcher());
        AttributeMatcher matcher2 = AttributeMatcher.vanilla(new WhitelistMatcher(Arrays.asList("a", "b")));

        CombiningMatcher combiner = new CombiningMatcher(MatcherCombiner.AND, Arrays.asList(matcher1, matcher2));

        assertThat(combiner.match("a", null, null, null), is(true));
        assertThat(combiner.match("b", null, Collections.emptyMap(), null), is(true));
        assertThat(combiner.match("c", null, null, null), is(false));
    }

}
