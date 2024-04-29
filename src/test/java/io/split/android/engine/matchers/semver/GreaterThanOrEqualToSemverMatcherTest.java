package io.split.android.engine.matchers.semver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Collections;

public class GreaterThanOrEqualToSemverMatcherTest {

    @Test
    public void matchShouldReturnTrueWhenKeyIsGreater() {
        GreaterThanOrEqualToSemverMatcher matcher = new GreaterThanOrEqualToSemverMatcher("1.2.3");

        boolean result = matcher.match("1.2.4", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchShouldReturnTrueWhenKeyIsEqual() {
        GreaterThanOrEqualToSemverMatcher matcher = new GreaterThanOrEqualToSemverMatcher("1.2.3");

        boolean result = matcher.match("1.2.3", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchShouldReturnFalseWhenKeyIsLess() {
        GreaterThanOrEqualToSemverMatcher matcher = new GreaterThanOrEqualToSemverMatcher("1.2.3");

        boolean result = matcher.match("1.2.2", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithPreReleaseShouldReturnTrueWhenEqual() {
        GreaterThanOrEqualToSemverMatcher matcher = new GreaterThanOrEqualToSemverMatcher("1.2.3----RC-SNAPSHOT.12.9.1--.12.88");

        boolean result = matcher.match("1.2.3----RC-SNAPSHOT.12.9.1--.12.88", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWithPreReleaseShouldReturnTrueWhenGreater() {
        GreaterThanOrEqualToSemverMatcher matcher = new GreaterThanOrEqualToSemverMatcher("1.2.3----RC-SNAPSHOT.12.9.1--.12.88");

        boolean result = matcher.match("1.2.3----RC-SNAPSHOT.12.9.1--.12.89", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWithPreReleaseShouldReturnFalseWhenLess() {
        GreaterThanOrEqualToSemverMatcher matcher = new GreaterThanOrEqualToSemverMatcher("1.2.3----RC-SNAPSHOT.12.9.1--.12.88");

        boolean result = matcher.match("1.2.3----RC-SNAPSHOT.12.9.1--.12.87", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithMetadataShouldReturnTrueWhenEqual() {
        GreaterThanOrEqualToSemverMatcher matcher = new GreaterThanOrEqualToSemverMatcher("2.2.2-rc.2+metadata-lalala");

        boolean result = matcher.match("2.2.2-rc.2+metadata-lalala", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWithMetadataShouldReturnTrueWhenGreater() {
        GreaterThanOrEqualToSemverMatcher matcher = new GreaterThanOrEqualToSemverMatcher("2.2.2-rc.2+metadata-lalala");

        boolean result = matcher.match("2.2.2-rc.3+metadata-lalala", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWithMetadataShouldReturnFalseWhenLess() {
        GreaterThanOrEqualToSemverMatcher matcher = new GreaterThanOrEqualToSemverMatcher("2.2.2-rc.2+metadata-lalala");

        boolean result = matcher.match("2.2.2-rc.1+metadata-lalala", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchShouldReturnFalseWhenKeyIsNull() {
        GreaterThanOrEqualToSemverMatcher matcher = new GreaterThanOrEqualToSemverMatcher("1.2.3");

        boolean result = matcher.match(null, null, null, null);

        assertFalse(result);
    }

    @Test
    public void generalUnsuccessfulMatches() {
        GreaterThanOrEqualToSemverMatcher matcher = new GreaterThanOrEqualToSemverMatcher("2.2.2-rc.2+metadata");

        assertFalse(matcher.match(10, null, null, null));
        assertFalse(matcher.match(true, null, null, null));
        assertFalse(matcher.match(Collections.singletonList("value"), null, null, null));
        assertFalse(matcher.match(LocalDateTime.now(), null, null, null));
    }
}
