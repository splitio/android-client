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
        validator = new KeyValidator("KeyValidatorTests");
        validator.setMessageLogger(Mockito.mock(ValidationMessageLogger.class));
    }

    @Test
    public void testValidMatchingKey() {
        Key key = new Key("key1", null);
        Assert.assertTrue(key.isValid(validator));
    }

    @Test
    public void testValidMatchingAndBucketingKey() {
        Key key = new Key("key1", "bkey1");
        Assert.assertTrue(key.isValid(validator));
    }

    @Test
    public void testNullMatchingKey() {
        Key key = new Key(null, null);
        Assert.assertFalse(key.isValid(validator));
    }

    @Test
    public void testInvalidEmptyMatchingKey() {
        Key key = new Key("", null);
        Assert.assertFalse(key.isValid(validator));
    }

    @Test
    public void testInvalidLongMatchingKey() {
        Key key = new Key(Strings.repeat("p", 256), null);
        Assert.assertFalse(key.isValid(validator));
    }

    @Test
    public void testInvalidEmptyBucketingKey() {
        Key key = new Key("key1", "");
        Assert.assertFalse(key.isValid(validator));
    }

    @Test
    public void testInvalidLongBucketingKey() {
        Key key = new Key("key1", Strings.repeat("p", 256));
        Assert.assertFalse(key.isValid(validator));
    }
}
