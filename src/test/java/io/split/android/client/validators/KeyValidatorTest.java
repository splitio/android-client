package io.split.android.client.validators;

import com.google.common.base.Strings;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.split.android.client.api.Key;

public class KeyValidatorTest {

    private KeyValidator validator;
    private static final String tag = "KeyValidatorTest";

    @Before
    public void setUp() {
        validator = new KeyValidatorImpl();
        validator.setMessageLogger(Mockito.mock(ValidationMessageLogger.class));
    }

    @Test
    public void testValidMatchingKey() {
        Assert.assertTrue(validator.isValidKey("key1", null, tag));
    }

    @Test
    public void testValidMatchingAndBucketingKey() {
        Assert.assertTrue(validator.isValidKey("key1", "bkey1", tag));
    }

    @Test
    public void testNullMatchingKey() {
        Assert.assertFalse(validator.isValidKey(null, null, tag));
    }

    @Test
    public void testInvalidEmptyMatchingKey() {
        Assert.assertFalse(validator.isValidKey("", null, tag));
    }

    @Test
    public void testInvalidAllSpacesInMatchingKey() {
        Assert.assertFalse(validator.isValidKey("   ", null, tag));
    }

    @Test
    public void testInvalidLongMatchingKey() {
        Assert.assertFalse(validator.isValidKey(Strings.repeat("p", 256), null, tag));
    }

    @Test
    public void testInvalidEmptyBucketingKey() {
        Assert.assertFalse(validator.isValidKey("key1", "", tag));
    }

    @Test
    public void testInvalidAllSpacesInBucketingKey() {
        Assert.assertFalse(validator.isValidKey("key1", "   ", tag));
    }

    @Test
    public void testInvalidLongBucketingKey() {
        Assert.assertFalse(validator.isValidKey("key1", Strings.repeat("p", 256), tag));
    }
}
