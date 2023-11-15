package io.split.android.client.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class PrefixValidatorImplTest {

    private PrefixValidatorImpl mValidator;

    @Before
    public void setUp() {
        mValidator = new PrefixValidatorImpl();
    }

    @Test
    public void validateNullPrefix() {
        ValidationErrorInfo result = mValidator.validate(null);

        assertTrue(result.isError());
        assertEquals("You passed a null prefix, prefix must be a non-empty string", result.getErrorMessage());
    }

    @Test
    public void validateEmptyPrefix() {
        ValidationErrorInfo result = mValidator.validate("");

        assertTrue(result.isError());
        assertEquals("You passed an empty prefix, prefix must be a non-empty string", result.getErrorMessage());
    }

    @Test
    public void validatePrefixTooLong() {
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < 81; i++) {
            prefix.append("a");
        }

        ValidationErrorInfo result = mValidator.validate(prefix.toString());

        assertTrue(result.isError());
        assertEquals("Prefix can only contain alphanumeric characters and underscore, and must be 80 characters or less", result.getErrorMessage());
    }

    @Test
    public void validateWhitespacePrefix() {
        ValidationErrorInfo result = mValidator.validate(" ");

        assertTrue(result.isError());
        assertEquals("You passed an empty prefix, prefix must be a non-empty string", result.getErrorMessage());
    }

    @Test
    public void validateSpecialCharPrefix() {
        ValidationErrorInfo result = mValidator.validate("a!b");

        assertTrue(result.isError());
        assertEquals("Prefix can only contain alphanumeric characters and underscore, and must be 80 characters or less", result.getErrorMessage());
    }

    @Test
    public void validateWithUnderscores() {
        ValidationErrorInfo result = mValidator.validate("a_b____");

        assertNull(result);
    }

    @Test
    public void validateValidPrefix() {
        ValidationErrorInfo result = mValidator.validate("_ab99_");

        assertNull(result);
    }
}
