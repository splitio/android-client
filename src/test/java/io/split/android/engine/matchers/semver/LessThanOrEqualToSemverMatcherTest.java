package io.split.android.engine.matchers.semver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Collections;

public class LessThanOrEqualToSemverMatcherTest {

    @Test
    public void matchShouldReturnFalseWhenGreater() {

        LessThanOrEqualToSemverMatcher matcher = new LessThanOrEqualToSemverMatcher("1.2.3");

        boolean result = matcher.match("1.2.4", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchShouldReturnTrueWhenEqual() {

        LessThanOrEqualToSemverMatcher matcher = new LessThanOrEqualToSemverMatcher("1.2.3");

        boolean result = matcher.match("1.2.3", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchShouldReturnTrueWhenLess() {

        LessThanOrEqualToSemverMatcher matcher = new LessThanOrEqualToSemverMatcher("1.2.3");

        boolean result = matcher.match("1.2.2", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWithPreReleaseShouldReturnTrue() {

        LessThanOrEqualToSemverMatcher matcher = new LessThanOrEqualToSemverMatcher("1.2.3----RC-SNAPSHOT.12.9.1--.12.89");

        boolean result = matcher.match("1.2.3----RC-SNAPSHOT.12.9.1--.12.88", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWithMetadataShouldReturnTrue() {

        LessThanOrEqualToSemverMatcher matcher = new LessThanOrEqualToSemverMatcher("2.2.2-rc.2+metadata-lalala");

        boolean result = matcher.match("2.2.2-rc.2+metadata", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWithMetadataShouldReturnFalse() {

        LessThanOrEqualToSemverMatcher matcher = new LessThanOrEqualToSemverMatcher("2.2.2-rc.2+metadata-lalala");

        boolean result = matcher.match("2.2.2-rc.3+metadata", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithNullTargetShouldReturnFalse() {

        LessThanOrEqualToSemverMatcher matcher = new LessThanOrEqualToSemverMatcher(null);

        boolean result = matcher.match("1.2.3", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithNullKeyShouldReturnNull() {

        LessThanOrEqualToSemverMatcher matcher = new LessThanOrEqualToSemverMatcher("1.2.3");

        boolean result = matcher.match(null, null, null, null);

        assertFalse(result);
    }

    @Test
    public void generalMatches() {
        LessThanOrEqualToSemverMatcher matcher = new LessThanOrEqualToSemverMatcher("1.2.3");

        assertFalse(matcher.match("2.2.3", null, null, null));
        assertFalse(matcher.match(10, null, null, null));
        assertFalse(matcher.match(Collections.singletonList("value"), null, null, null));
        assertFalse(matcher.match(LocalDateTime.now(), null, null, null));
        assertFalse(matcher.match(true, null, null, null));
        assertTrue(matcher.match("1.2.3-rc1", null, null, null));
    }
}
