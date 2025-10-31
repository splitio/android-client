package io.split.android.engine.matchers.semver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Collections;

public class EqualToSemverMatcherTest {

    @Test
    public void matchShouldReturnFalseWhenPatchDiffers() {
        EqualToSemverMatcher equalToSemverMatcher = new EqualToSemverMatcher("1.0.0");

        boolean result = equalToSemverMatcher.match("1.0.1", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchShouldReturnTrueWhenVersionsAreEqual() {
        EqualToSemverMatcher equalToSemverMatcher = new EqualToSemverMatcher("1.1.2");

        boolean result = equalToSemverMatcher.match("1.1.2", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWithPreReleaseShouldReturnTrueWhenVersionsAreEqual() {
        EqualToSemverMatcher equalToSemverMatcher = new EqualToSemverMatcher("1.2.3----RC-SNAPSHOT.12.9.1--.12.88");

        boolean result = equalToSemverMatcher.match("1.2.3----RC-SNAPSHOT.12.9.1--.12.88", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWithPreReleaseShouldReturnFalseWhenVersionsDiffer() {
        EqualToSemverMatcher equalToSemverMatcher = new EqualToSemverMatcher("1.2.3----RC-SNAPSHOT.12.9.1--.12.88");

        boolean result = equalToSemverMatcher.match("1.2.3----RC-SNAPSHOT.12.9.1--.12.99", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchWithMetadataShouldReturnTrueWhenVersionsAreEqual() {
        EqualToSemverMatcher equalToSemverMatcher = new EqualToSemverMatcher("2.2.2-rc.2+metadata-lalala");

        boolean result = equalToSemverMatcher.match("2.2.2-rc.2+metadata-lalala", null, null, null);

        assertTrue(result);
    }

    @Test
    public void matchWithMetadataShouldReturnFalseWhenVersionsDiffer() {
        EqualToSemverMatcher equalToSemverMatcher = new EqualToSemverMatcher("2.2.2-rc.2+metadata-lalala");

        boolean result = equalToSemverMatcher.match("2.2.2-rc.2+metadata", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchShouldReturnFalseWhenTargetIsNull() {
        EqualToSemverMatcher equalToSemverMatcher = new EqualToSemverMatcher(null);

        boolean result = equalToSemverMatcher.match("1.0.0", null, null, null);

        assertFalse(result);
    }

    @Test
    public void matchShouldReturnFalseWhenKeyIsNull() {
        EqualToSemverMatcher equalToSemverMatcher = new EqualToSemverMatcher("1.0.0");

        boolean result = equalToSemverMatcher.match(null, null, null, null);

        assertFalse(result);
    }

    @Test
    public void generalUnsuccessfulMatches() {
        EqualToSemverMatcher equalToSemverMatcher = new EqualToSemverMatcher("2.2.2-rc.2+metadata");

        assertFalse(equalToSemverMatcher.match(10, null, null, null));
        assertFalse(equalToSemverMatcher.match(true, null, null, null));
        assertFalse(equalToSemverMatcher.match(Collections.singletonList("value"), null, null, null));
        assertFalse(equalToSemverMatcher.match(LocalDateTime.now(), null, null, null));
    }
}
