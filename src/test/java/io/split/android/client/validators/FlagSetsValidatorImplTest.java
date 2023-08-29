package io.split.android.client.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FlagSetsValidatorImplTest {

    private final FlagSetsValidatorImpl mValidator = new FlagSetsValidatorImpl();

    @Test
    public void nullInputReturnsEmptyList() {
        List<String> result = mValidator.cleanup(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void emptyInputReturnsEmptyList() {
        List<String> result = mValidator.cleanup(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void duplicatedInputValuesAreRemoved() {
        List<String> result = mValidator.cleanup(Arrays.asList("set1", "set1"));
        assertEquals(1, result.size());
        assertTrue(result.contains("set1"));
    }

    @Test
    public void valuesAreSortedAlphanumerically() {
        List<String> result = mValidator.cleanup(Arrays.asList("set2", "set1", "set_1", "1set"));
        assertEquals(4, result.size());
        assertEquals("1set", result.get(0));
        assertEquals("set1", result.get(1));
        assertEquals("set2", result.get(2));
        assertEquals("set_1", result.get(3));
    }

    @Test
    public void invalidValuesAreRemoved() {
        List<String> result = mValidator.cleanup(Arrays.asList("set1", "set2", "set_1", "set-1", "set 1", "set 2"));
        assertEquals(3, result.size());
        assertEquals("set1", result.get(0));
        assertEquals("set2", result.get(1));
        assertEquals("set_1", result.get(2));
    }

    @Test
    public void setWithMoreThan50CharsIsRemoved() {
        String longSet = "abcdfghijklmnopqrstuvwxyz1234567890abcdfghijklmnopq";
        List<String> result = mValidator.cleanup(Arrays.asList("set1", longSet));
        assertEquals(51, longSet.length());
        assertEquals(1, result.size());
        assertEquals("set1", result.get(0));
    }

    @Test
    public void setWithLessThanOneCharIsOrEmptyRemoved() {
        List<String> result = mValidator.cleanup(Arrays.asList("set1", "", " "));
        assertEquals(1, result.size());
        assertEquals("set1", result.get(0));
    }

    @Test
    public void nullSetIsRemoved() {
        List<String> result = mValidator.cleanup(Arrays.asList("set1", null));
        assertEquals(1, result.size());
        assertEquals("set1", result.get(0));
    }

    @Test
    public void setWithExtraWhitespaceIsTrimmed() {
        List<String> result = mValidator.cleanup(Arrays.asList("set1 ", " set2\r", "set3  ", "set 4\n"));
        assertEquals(3, result.size());
        assertEquals("set1", result.get(0));
        assertEquals("set2", result.get(1));
        assertEquals("set3", result.get(2));
    }

    @Test
    public void setsAreLowercase() {
        List<String> result = mValidator.cleanup(Arrays.asList("SET1", "Set2", "SET_3"));
        assertEquals(3, result.size());
        assertEquals("set1", result.get(0));
        assertEquals("set2", result.get(1));
        assertEquals("set_3", result.get(2));
    }
}
