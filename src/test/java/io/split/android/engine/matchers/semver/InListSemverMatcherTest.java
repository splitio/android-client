package io.split.android.engine.matchers.semver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

public class InListSemverMatcherTest {

    @Test
    public void matchShouldReturnTrueWhenInList() {

        InListSemverMatcher matcher = new InListSemverMatcher(Arrays.asList("1.2.3", "1.2.5", "1.2.4"));

        boolean result = matcher.match("1.2.4", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchShouldReturnFalseWhenNotInList() {

        InListSemverMatcher matcher = new InListSemverMatcher(Arrays.asList("1.2.3", "1.2.5", "1.2.4"));

        boolean result = matcher.match("1.2.6", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithPreReleaseShouldReturnTrueWhenInList() {

        InListSemverMatcher matcher = new InListSemverMatcher(Arrays.asList("1.1.1-rc.1.1.1", "1.1.1-rc.1.1.3", "1.1.1-rc.1.1.2"));

        boolean result = matcher.match("1.1.1-rc.1.1.2", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWithPreReleaseShouldReturnFalseWhenNotInList() {

        InListSemverMatcher matcher = new InListSemverMatcher(Arrays.asList("1.1.1-rc.1.1.1", "1.1.1-rc.1.1.3", "1.1.1-rc.1.1.2"));

        boolean result = matcher.match("1.1.1-rc.1.1.4", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithMetadataShouldReturnTrueWhenInList() {

        InListSemverMatcher matcher = new InListSemverMatcher(Arrays.asList("1.2.3+meta", "1.2.5+meta", "1.2.4+meta"));

        boolean result = matcher.match("1.2.4+meta", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWithMetadataShouldReturnFalseWhenNotInList() {

        InListSemverMatcher matcher = new InListSemverMatcher(Arrays.asList("1.2.3+meta", "1.2.5+meta", "1.2.4+meta"));

        boolean result = matcher.match("1.2.6+meta", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithNullTargetShouldReturnFalse() {

        InListSemverMatcher matcher = new InListSemverMatcher(null);

        boolean result = matcher.match("1.2.6+meta", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithEmptyListShouldReturnFalse() {

        InListSemverMatcher matcher = new InListSemverMatcher(Collections.emptyList());

        boolean result = matcher.match("1.2.6+meta", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithNullKeyShouldReturnFalse() {

        InListSemverMatcher matcher = new InListSemverMatcher(Arrays.asList("1.2.3+meta", "1.2.5+meta", "1.2.4+meta"));

        boolean result = matcher.match(null, null, null, null);

        assertFalse(result);
    }

    @Test
    public void generalMatches() {
        InListSemverMatcher matcher = new InListSemverMatcher(Arrays.asList("1.2.3", "1.2.5", "1.2.4"));

        assertTrue(matcher.match("1.2.4", null, null, null));
        assertFalse(matcher.match(10, null, null, null));
        assertFalse(matcher.match(Collections.singletonList("value"), null, null, null));
        assertFalse(matcher.match(LocalDateTime.now(), null, null, null));
        assertFalse(matcher.match(false, null, null, null));
        assertFalse(matcher.match("1.2.6", null, null, null));
    }
}
