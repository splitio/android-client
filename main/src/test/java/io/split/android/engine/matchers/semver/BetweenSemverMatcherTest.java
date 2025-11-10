package io.split.android.engine.matchers.semver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Collections;

public class BetweenSemverMatcherTest {

    @Test
    public void matchShouldReturnTrueWhenBetween() {

        BetweenSemverMatcher matcher = new BetweenSemverMatcher("1.2.3", "1.2.5");

        boolean result = matcher.match("1.2.4", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchShouldReturnFalseWhenLess() {

        BetweenSemverMatcher matcher = new BetweenSemverMatcher("1.2.3", "1.2.5");

        boolean result = matcher.match("1.2.2", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchShouldReturnFalseWhenGreater() {

        BetweenSemverMatcher matcher = new BetweenSemverMatcher("1.2.3", "1.2.5");

        boolean result = matcher.match("1.2.6", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithPreReleaseShouldReturnTrueWhenBetween() {

        BetweenSemverMatcher matcher = new BetweenSemverMatcher("1.1.1-rc.1.1.1", "1.1.1-rc.1.1.3");

        boolean result = matcher.match("1.1.1-rc.1.1.2", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWiehPreReleaseShouldReturnFalseWhenLess() {

        BetweenSemverMatcher matcher = new BetweenSemverMatcher("1.1.1-rc.1.1.1", "1.1.1-rc.1.1.3");

        boolean result = matcher.match("1.1.1-rc.1.1.0", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithPreReleaseShouldReturnFalseWhenGreater() {

        BetweenSemverMatcher matcher = new BetweenSemverMatcher("1.1.1-rc.1.1.1", "1.1.1-rc.1.1.3");

        boolean result = matcher.match("1.1.1-rc.1.1.4", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithMetadataShouldReturnFalseWhenLess() {

        BetweenSemverMatcher matcher = new BetweenSemverMatcher("2.2.2-rc.3+metadata-lalala", "2.2.2-rc.4+metadata-lalala");

        boolean result = matcher.match("2.2.2-rc.2+metadata-lalala", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithMetadataShouldReturnFalseWhenGreater() {

        BetweenSemverMatcher matcher = new BetweenSemverMatcher("2.2.2-rc.3+metadata-lalala", "2.2.2-rc.4+metadata-lalala");

        boolean result = matcher.match("2.2.2-rc.5+metadata-lalala", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithMetadataShouldReturnTrueWhenBetween() {

        BetweenSemverMatcher matcher = new BetweenSemverMatcher("2.2.2-rc.2+metadata-lalala", "2.2.2-rc.4+metadata-lalala");

        boolean result = matcher.match("2.2.2-rc.3+metadata-lalala", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWithNullStartTargetShouldReturnFalse() {

        BetweenSemverMatcher matcher = new BetweenSemverMatcher(null, "1.2.5");

        boolean result = matcher.match("1.2.4", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithNullEndTargetShouldReturnFalse() {

        BetweenSemverMatcher matcher = new BetweenSemverMatcher("1.2.3", null);

        boolean result = matcher.match("1.2.4", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithNullKeyShouldReturnFalse() {

        BetweenSemverMatcher matcher = new BetweenSemverMatcher("1.2.3", "1.2.5");

        boolean result = matcher.match(null, null, null, null);

        assertFalse(result);
    }

    @Test
    public void generalMatches() {

        BetweenSemverMatcher matcher = new BetweenSemverMatcher("2.2.2+metadata-lalala", "3.4.5+metadata-lalala");

        assertTrue(matcher.match("2.2.3", null, null, null));
        assertFalse(matcher.match(10, null, null, null));
        assertFalse(matcher.match(Collections.singletonList("value"), null, null, null));
        assertFalse(matcher.match(LocalDateTime.now(), null, null, null));
        assertFalse(matcher.match(false, null, null, null));
        assertFalse(matcher.match("5.2.2-rc.1", null, null, null));
    }
}
