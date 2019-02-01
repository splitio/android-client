package io.split.android.client.validators;

import com.google.common.base.Strings;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.split.android.client.dtos.Split;

public class SplitNameValidatorTest {

    private SplitValidator validator;

    @Before
    public void setUp() {
        validator = new SplitValidatorImpl("SplitNameValidatorTests");
        validator.setMessageLogger(Mockito.mock(ValidationMessageLogger.class));
    }

    @Test
    public void testValidName() {
        String splitName = "split1";
        Assert.assertTrue(validator.isValidName(splitName));
        Assert.assertFalse(validator.nameHasToBeTrimmed(splitName));
    }

    @Test
    public void testNullName() {
        Assert.assertFalse(validator.isValidName(null));
    }

    @Test
    public void testInvalidEmptyName() {
        Assert.assertFalse(validator.isValidName(""));
    }

    @Test
    public void testLeadingSpacesName() {
        String splitName = " splitName";
        Assert.assertTrue(validator.isValidName(splitName));
        Assert.assertTrue(validator.nameHasToBeTrimmed(splitName));
    }

    @Test
    public void testTrailingSpacesName() {
        String splitName = "splitName ";
        Assert.assertTrue(validator.isValidName(splitName));
        Assert.assertTrue(validator.nameHasToBeTrimmed(splitName));
    }
}
