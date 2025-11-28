package io.split.android.client.events.metadata;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MetadataValidatorImplTest {

    private MetadataValidator mValidator;

    @Before
    public void setUp() {
        mValidator = new MetadataValidatorImpl();
    }

    @Test
    public void isValidValueReturnsTrueForString() {
        assertTrue(mValidator.isValidValue("value"));
    }

    @Test
    public void isValidValueReturnsTrueForEmptyString() {
        assertTrue(mValidator.isValidValue(""));
    }

    @Test
    public void isValidValueReturnsTrueForInteger() {
        assertTrue(mValidator.isValidValue(42));
    }

    @Test
    public void isValidValueReturnsTrueForLong() {
        assertTrue(mValidator.isValidValue(1234567890L));
    }

    @Test
    public void isValidValueReturnsTrueForDouble() {
        assertTrue(mValidator.isValidValue(3.14));
    }

    @Test
    public void isValidValueReturnsTrueForFloat() {
        assertTrue(mValidator.isValidValue(2.5f));
    }

    @Test
    public void isValidValueReturnsTrueForBooleanTrue() {
        assertTrue(mValidator.isValidValue(true));
    }

    @Test
    public void isValidValueReturnsTrueForBooleanFalse() {
        assertTrue(mValidator.isValidValue(false));
    }

    @Test
    public void isValidValueReturnsTrueForListOfStrings() {
        List<String> list = Arrays.asList("flag_1", "flag_2", "flag_3");
        assertTrue(mValidator.isValidValue(list));
    }

    @Test
    public void isValidValueReturnsTrueForEmptyList() {
        assertTrue(mValidator.isValidValue(Collections.emptyList()));
    }

    @Test
    public void isValidValueReturnsTrueForSingleElementStringList() {
        assertTrue(mValidator.isValidValue(Collections.singletonList("single")));
    }

    @Test
    public void isValidValueReturnsFalseForNull() {
        assertFalse(mValidator.isValidValue(null));
    }

    @Test
    public void isValidValueReturnsFalseForListWithNullElement() {
        List<String> list = Arrays.asList("valid", null, "also_valid");
        assertFalse(mValidator.isValidValue(list));
    }

    @Test
    public void isValidValueReturnsFalseForListWithMixedTypes() {
        List<Object> mixedList = Arrays.asList("string", 123, true);
        assertFalse(mValidator.isValidValue(mixedList));
    }

    @Test
    public void isValidValueReturnsFalseForListOfIntegers() {
        List<Integer> intList = Arrays.asList(1, 2, 3);
        assertFalse(mValidator.isValidValue(intList));
    }

    @Test
    public void isValidValueReturnsFalseForListOfBooleans() {
        List<Boolean> boolList = Arrays.asList(true, false, true);
        assertFalse(mValidator.isValidValue(boolList));
    }

    @Test
    public void isValidValueReturnsFalseForPlainObject() {
        assertFalse(mValidator.isValidValue(new Object()));
    }

    @Test
    public void isValidValueReturnsFalseForMap() {
        assertFalse(mValidator.isValidValue(new HashMap<String, String>()));
    }

    @Test
    public void isValidValueReturnsFalseForNestedList() {
        List<List<String>> nestedList = Arrays.asList(
                Arrays.asList("a", "b"),
                Arrays.asList("c", "d")
        );
        assertFalse(mValidator.isValidValue(nestedList));
    }

    @Test
    public void isValidValueReturnsFalseForArray() {
        String[] array = {"a", "b", "c"};
        assertFalse(mValidator.isValidValue(array));
    }
}
