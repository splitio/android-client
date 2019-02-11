package io.split.android.client.validators;

import com.google.common.base.Strings;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.split.android.client.dtos.Split;

public class SplitNameValidatorTest {

    private SplitValidator validator;
    private final  String  tag = "SplitNameValidatorTests";

    @Before
    public void setUp() {
        validator = new SplitValidatorImpl();
        validator.setMessageLogger(Mockito.mock(ValidationMessageLogger.class));
    }

    @Test
    public void testValidName() {
        String splitName = "split1";
        Assert.assertTrue(validator.isValidName(splitName, tag));
        Assert.assertEquals(splitName, validator.trimName(splitName, tag));
    }

    @Test
    public void testNullName() {
        Assert.assertFalse(validator.isValidName(null, tag));
    }

    @Test
    public void testInvalidEmptyName() {
        Assert.assertFalse(validator.isValidName("", tag));
    }

    @Test
    public void testLeadingSpacesName() {
        String splitName = " splitName";
        Assert.assertTrue(validator.isValidName(splitName, tag));
        Assert.assertFalse(splitName.equals(validator.trimName(splitName, tag)));
    }

    @Test
    public void testTrailingSpacesName() {
        String splitName = "splitName ";
        Assert.assertTrue(validator.isValidName(splitName, tag));
        Assert.assertFalse(splitName.equals(validator.trimName(splitName, tag)));
    }
}
