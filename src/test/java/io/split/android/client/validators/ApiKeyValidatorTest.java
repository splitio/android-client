package io.split.android.client.validators;

import com.google.common.base.Strings;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.split.android.client.api.Key;

public class ApiKeyValidatorTest {

    private ApiKeyValidator validator;

    @Before
    public void setUp() {
        validator = new ApiKeyValidator("KeyValidatorTests");
        validator.setMessageLogger(Mockito.mock(ValidationMessageLogger.class));
    }

    @Test
    public void testValidKey() {
        ApiKeyValidatable key = new ApiKeyValidatable("key1");
        Assert.assertTrue(key.isValid(validator));
    }

    @Test
    public void testNullKey() {
        ApiKeyValidatable key = new ApiKeyValidatable(null);
        Assert.assertFalse(key.isValid(validator));
    }

    @Test
    public void testInvalidEmptyKey() {
        ApiKeyValidatable key = new ApiKeyValidatable("");
        Assert.assertFalse(key.isValid(validator));
    }
}
