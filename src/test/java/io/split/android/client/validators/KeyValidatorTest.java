package io.split.android.client.validators;

import com.google.common.base.Strings;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

public class KeyValidatorTest {

    private KeyValidator validator;
    private static final String tag = "KeyValidatorTest";

    @Before
    public void setUp() {
        validator = new KeyValidatorImpl();
    }

    @Test
    public void testValidMatchingKey() {

        ValidationErrorInfo errorInfo = validator.validate("key1", null);

        Assert.assertNull(errorInfo);
    }

    @Test
    public void testValidMatchingAndBucketingKey() {
        ValidationErrorInfo errorInfo = validator.validate("key1", "bkey1");

        Assert.assertNull(errorInfo);
    }

    @Test
    public void testNullMatchingKey() {
        ValidationErrorInfo errorInfo = validator.validate(null, null);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed a null key, matching key must be a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testInvalidEmptyMatchingKey() {
        ValidationErrorInfo errorInfo = validator.validate("", null);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed an empty string, matching key must be a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testInvalidAllSpacesInMatchingKey() {
        ValidationErrorInfo errorInfo = validator.validate("     ", null);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed an empty string, matching key must be a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testInvalidLongMatchingKey() {
        ValidationErrorInfo errorInfo = validator.validate(Strings.repeat("p", 256), null);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("matching key too long - must be " + ValidationConfig.getInstance().getMaximumKeyLength() + " characters or less", errorInfo.getErrorMessage());
    }

    @Test
    public void testInvalidEmptyBucketingKey() {
        ValidationErrorInfo errorInfo = validator.validate("key1", "");

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed an empty string, bucketing key must be null or a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testInvalidAllSpacesInBucketingKey() {
        ValidationErrorInfo errorInfo = validator.validate("key1", "    ");

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed an empty string, bucketing key must be null or a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testInvalidLongBucketingKey() {
        ValidationErrorInfo errorInfo = validator.validate("key1", Strings.repeat("p", 256));

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("bucketing key too long - must be " + ValidationConfig.getInstance().getMaximumKeyLength() + " characters or less", errorInfo.getErrorMessage());
    }
}
