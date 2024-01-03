package io.split.android.engine.matchers;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.Arrays;

import io.split.android.engine.matchers.strings.WhitelistMatcher;

/**
 * Tests for NegatableMatcher.
 *
 */
public class NegatableMatcherTest {

    @Test
    public void works_all_keys() {
        AllKeysMatcher delegate = new AllKeysMatcher();
        AttributeMatcher.NegatableMatcher matcher = new AttributeMatcher.NegatableMatcher(delegate, true);

        test(matcher, "foo", false);
    }

    @Test
    public void works_whitelist() {
        WhitelistMatcher delegate = new WhitelistMatcher(Arrays.asList("a", "b"));
        AttributeMatcher.NegatableMatcher matcher = new AttributeMatcher.NegatableMatcher(delegate, true);

        test(matcher, "a", false);
        test(matcher, "b", false);
        test(matcher, "c", true);
    }

    private void test(AttributeMatcher.NegatableMatcher negationMatcher, String key, boolean expected) {
        assertThat(negationMatcher.match(key, null, null, null), is(expected));
        assertThat(negationMatcher.delegate().match(key, null, null, null), is(!expected));

    }


}
