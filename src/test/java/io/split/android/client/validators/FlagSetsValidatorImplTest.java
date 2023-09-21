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
        SplitFilterValidator.ValidationResult result = mValidator.cleanup("method", null);
        assertTrue(result.getValues().isEmpty());
        assertEquals(0, result.getInvalidValueCount());
    }

    @Test
    public void emptyInputReturnsEmptyList() {
        SplitFilterValidator.ValidationResult result = mValidator.cleanup("method", Collections.emptyList());
        assertTrue(result.getValues().isEmpty());
        assertEquals(0, result.getInvalidValueCount());
    }

    @Test
    public void duplicatedInputValuesAreRemoved() {
        SplitFilterValidator.ValidationResult result = mValidator.cleanup("method", Arrays.asList("set1", "set1"));
        assertEquals(1, result.getValues().size());
        assertTrue(result.getValues().contains("set1"));
        assertEquals(0, result.getInvalidValueCount());
    }

    @Test
    public void valuesAreSortedAlphanumerically() {
        SplitFilterValidator.ValidationResult result = mValidator.cleanup("method", Arrays.asList("set2", "set1", "set_1", "1set"));
        assertEquals(4, result.getValues().size());
        assertEquals("1set", result.getValues().get(0));
        assertEquals("set1", result.getValues().get(1));
        assertEquals("set2", result.getValues().get(2));
        assertEquals("set_1", result.getValues().get(3));
        assertEquals(0, result.getInvalidValueCount());
    }

    @Test
    public void invalidValuesAreRemoved() {
        SplitFilterValidator.ValidationResult result = mValidator.cleanup("method", Arrays.asList("set1", "set2", "set_1", "set-1", "set 1", "set 2"));
        assertEquals(3, result.getValues().size());
        assertEquals("set1", result.getValues().get(0));
        assertEquals("set2", result.getValues().get(1));
        assertEquals("set_1", result.getValues().get(2));
        assertEquals(3, result.getInvalidValueCount());
    }

    @Test
    public void setWithMoreThan50CharsIsRemoved() {
        String longSet = "abcdfghijklmnopqrstuvwxyz1234567890abcdfghijklmnopq";
        SplitFilterValidator.ValidationResult result = mValidator.cleanup("method", Arrays.asList("set1", longSet));
        assertEquals(51, longSet.length());
        assertEquals(1, result.getValues().size());
        assertEquals("set1", result.getValues().get(0));
        assertEquals(1, result.getInvalidValueCount());
    }

    @Test
    public void setWithLessThanOneCharIsOrEmptyRemoved() {
        SplitFilterValidator.ValidationResult result = mValidator.cleanup("method", Arrays.asList("set1", "", " "));
        assertEquals(1, result.getValues().size());
        assertEquals("set1", result.getValues().get(0));
        assertEquals(2, result.getInvalidValueCount());
    }

    @Test
    public void nullSetIsRemoved() {
        SplitFilterValidator.ValidationResult result = mValidator.cleanup("method", Arrays.asList("set1", null));
        assertEquals(1, result.getValues().size());
        assertEquals("set1", result.getValues().get(0));
        assertEquals(1, result.getInvalidValueCount());
    }

    @Test
    public void setWithExtraWhitespaceIsTrimmed() {
        SplitFilterValidator.ValidationResult result = mValidator.cleanup("method", Arrays.asList("set1 ", " set2\r", "set3  ", "set 4\n"));
        assertEquals(3, result.getValues().size());
        assertEquals("set1", result.getValues().get(0));
        assertEquals("set2", result.getValues().get(1));
        assertEquals("set3", result.getValues().get(2));
        assertEquals(1, result.getInvalidValueCount());
    }

    @Test
    public void setsAreLowercase() {
        SplitFilterValidator.ValidationResult result = mValidator.cleanup("method", Arrays.asList("SET1", "Set2", "SET_3"));
        assertEquals(3, result.getValues().size());
        assertEquals("set1", result.getValues().get(0));
        assertEquals("set2", result.getValues().get(1));
        assertEquals("set_3", result.getValues().get(2));
        assertEquals(0, result.getInvalidValueCount());
    }
}
