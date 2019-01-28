package io.split.android.client.validators;

import com.google.common.base.Strings;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.split.android.client.dtos.Split;

public class SplitNameValidatorTest {

    private SplitNameValidator validator;

    @Before
    public void setUp() {
        validator = new SplitNameValidator("SplitNameValidatorTests");
        validator.setMessageLogger(Mockito.mock(ValidationMessageLogger.class));
    }

    @Test
    public void testValidName() {
        Split split = new Split("split1");
        Assert.assertTrue(split.isValid(validator));
    }

    @Test
    public void testNullName() {
        Split split = new Split(null);
        Assert.assertFalse(split.isValid(validator));
    }

    @Test
    public void testInvalidEmptyName() {
        Split split = new Split("");
        Assert.assertFalse(split.isValid(validator));
    }
}
