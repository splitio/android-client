package io.split.android.client.utils;

import static junit.framework.TestCase.assertEquals;

import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RepeatTest {

    @Test
    public void repeatBase() {
        assertEquals("abcabcabc", Utils.repeat("abc", 3));
    }

    @Test
    public void repeatOnce() {
        assertEquals("abc", Utils.repeat("abc", 1));
    }

    @Test
    public void repeatZeroTimesReturnsEmptyString() {
        assertEquals("", Utils.repeat("abc", 0));
    }

    @Test
    public void repeatEmptyStringReturnsEmptyString() {
        assertEquals("", Utils.repeat("", 5));
    }

    @Test
    public void repeatNegativeCountReturnsInputString() {
        assertEquals("abc", Utils.repeat("abc", -1));
    }

    @Test
    public void repeatNullStringReturnsNull() {
        assertNull(Utils.repeat(null, 3));
    }
}
