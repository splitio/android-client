package io.split.android.client.validators;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class AttributesValidatorImplTest {

    private final AttributesValidatorImpl validator = new AttributesValidatorImpl();

    @Test
    public void stringIsValidValue() {
        assertValid("yes");
    }

    @Test
    public void longIsValidValue() {
        assertValid(100L);
    }

    @Test
    public void intIsValidValue() {
        assertValid(10);
    }

    @Test
    public void booleanIsValidValue() {
        assertValid(true);
        assertValid(false);
    }

    @Test
    public void collectionIsValidValue() {
        assertValid(new ArrayList<>());
    }

    private void assertValid(Object value) {
        Assert.assertTrue(validator.isValid(value));
    }
}
