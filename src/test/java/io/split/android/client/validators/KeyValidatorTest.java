package io.split.android.client.validators;

import com.google.common.base.Strings;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.split.android.client.api.Key;

public class KeyValidatorTest {

    private KeyValidator validator;

    @Before
    public void setUp() {
        validator = new KeyValidatorImpl("KeyValidatorTests");
        validator.setMessageLogger(Mockito.mock(ValidationMessageLogger.class));
    }

    @Test
    public void testValidMatchingKey() {
        Assert.assertTrue(validator.isValidKey(new Key("key1", null)));
    }

    @Test
    public void testValidMatchingAndBucketingKey() {
        Assert.assertTrue(validator.isValidKey(new Key("key1", "bkey1")));
    }

    @Test
    public void testNullMatchingKey() {
        Assert.assertFalse(validator.isValidKey(new Key(null, null)));
    }

    @Test
    public void testInvalidEmptyMatchingKey() {
        Assert.assertFalse(validator.isValidKey(new Key("", null)));
    }

    @Test
    public void testInvalidLongMatchingKey() {
        Assert.assertFalse(validator.isValidKey(new Key(Strings.repeat("p", 256), null)));
    }

    @Test
    public void testInvalidEmptyBucketingKey() {
        Assert.assertFalse(validator.isValidKey(new Key("key1", "")));
    }

    @Test
    public void testInvalidLongBucketingKey() {
        Assert.assertFalse(validator.isValidKey(new Key("key1", Strings.repeat("p", 256))));
    }
}
