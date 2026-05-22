package io.split.android.client.localhost;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class LocalhostTrafficTypeValidatorTest {

    private LocalhostTrafficTypeValidator mValidator;

    @Before
    public void setUp() {
        mValidator = new LocalhostTrafficTypeValidator();
    }

    @Test
    public void isValidReturnsTrueForAnyTrafficType() {
        assertTrue(mValidator.isValid("user"));
        assertTrue(mValidator.isValid("account"));
        assertTrue(mValidator.isValid("random_traffic_type"));
    }

    @Test
    public void isValidReturnsTrueForNull() {
        assertTrue(mValidator.isValid(null));
    }

    @Test
    public void isValidReturnsTrueForEmptyString() {
        assertTrue(mValidator.isValid(""));
    }

    @Test
    public void isValidReturnsTrueForWhitespace() {
        assertTrue(mValidator.isValid("   "));
    }

    @Test
    public void isValidReturnsTrueForSpecialCharacters() {
        assertTrue(mValidator.isValid("!@#$%^&*()"));
    }
}
