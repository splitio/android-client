package io.split.android.client.validators;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

public class ApiKeyValidatorTest {

    private ApiKeyValidator validator;

    @Before
    public void setUp() {
        validator = new ApiKeyValidatorImpl();
    }

    @Test
    public void testValidKey() {
        ValidationErrorInfo errorInfo = validator.validate("key1");

        Assert.assertNull(errorInfo);
    }

    @Test
    public void testNullKey() {
        ValidationErrorInfo errorInfo = validator.validate(null);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed a null sdkKey, the sdkKey must be a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testInvalidEmptyKey() {
        ValidationErrorInfo errorInfo = validator.validate("");

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed an empty sdkKey, sdkKey must be a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testInvalidAllSpacesKey() {
        ValidationErrorInfo errorInfo = validator.validate("    ");

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed an empty sdkKey, sdkKey must be a non-empty string", errorInfo.getErrorMessage());
    }
}
