package io.split.android.engine.matchers.strings;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for WhitelistMatcher
 *
 */
public class WhitelistMatcherTest {

    @Test
    public void works() {
        List<String> whitelist = Arrays.asList("a", "a\"b");

        WhitelistMatcher matcher = new WhitelistMatcher(whitelist);

        for (String item : whitelist) {
            assertThat(matcher.match(item, null, null, null), is(true));
        }

        assertThat(matcher.match("hello", null, null, null), is(false));
        assertThat(matcher.match(null, null, null, null), is(false));
    }

    @Test
    public void works_with_empty_whitelist() {
        List<String> whitelist = Arrays.asList("a", "a\"b");

        WhitelistMatcher matcher = new WhitelistMatcher(Collections.emptyList());

        for (String item : whitelist) {
            assertThat(matcher.match(item, null, null, null), is(false));
        }

    }

}
