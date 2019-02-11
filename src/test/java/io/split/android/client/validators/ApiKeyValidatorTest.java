package io.split.android.client.validators;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ApiKeyValidatorTest {

    private ApiKeyValidator validator;

    @Before
    public void setUp() {
        validator = new ApiKeyValidatorImpl("KeyValidatorTests");
        validator.setMessageLogger(Mockito.mock(ValidationMessageLogger.class));
    }

    @Test
    public void testValidKey() {
        Assert.assertTrue(validator.isValidApiKey("key1"));
    }

    @Test
    public void testNullKey() {
        Assert.assertFalse(validator.isValidApiKey(null));
    }

    @Test
    public void testInvalidEmptyKey() {
        Assert.assertFalse(validator.isValidApiKey(""));
    }
}
