package io.split.android.client.attributes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.validators.AttributesValidator;
import io.split.android.client.validators.ValidationMessageLogger;

public class AttributesManagerImplTest {

    @Mock
    AttributesStorage attributesStorage;
    @Mock
    AttributesValidator attributesValidator;
    @Mock
    ValidationMessageLogger validationMessageLogger;

    private AttributesManagerImpl attributeClient;
    private Map<String, Object> testValues;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        attributeClient = new AttributesManagerImpl(attributesStorage, attributesValidator, validationMessageLogger);
        testValues = getDefaultValues();
    }

    @Test
    public void setAttributeUpdatesValueInStorageIfAttributeValueIsValid() {
        String name = "key";
        String attribute = "value";
        when(attributesValidator.isValid(attribute)).thenReturn(true);

        attributeClient.setAttribute(name, attribute);

        verify(attributesStorage).set(name, attribute);
    }

    @Test
    public void setAttributeReturnsTrueIfAttributeValueIsValid() {
        String name = "key";
        String attribute = "value";
        when(attributesValidator.isValid(attribute)).thenReturn(true);

        boolean result = attributeClient.setAttribute(name, attribute);

        Assert.assertTrue(result);
    }

    @Test
    public void setAttributeDoesNotSaveAttributeInStorageIfAttributeValueIsNotValid() {
        String name = "key";
        String attribute = "value";
        when(attributesValidator.isValid(attribute)).thenReturn(false);

        attributeClient.setAttribute(name, attribute);

        Mockito.verifyNoInteractions(attributesStorage);
    }

    @Test
    public void setAttributeReturnsFalseIfAttributeValueNotIsValid() {
        String name = "key";
        String attribute = "value";
        when(attributesValidator.isValid(attribute)).thenReturn(false);

        boolean result = attributeClient.setAttribute(name, attribute);

        Assert.assertFalse(result);
    }

    @Test
    public void setAttributeLogsWarningMessageIfValueIsNotValid() {
        when(attributesValidator.isValid(any(Object.class))).thenReturn(false);

        attributeClient.setAttribute("key", "invalidValue");

        verify(validationMessageLogger).w(eq("You passed an invalid attribute value for key, acceptable types are String, double, float, long, int, boolean or Collections"), any());
    }

    @Test
    public void getReturnsValueFetchedFromStorage() {
        String name = "key";
        int attribute = 100;
        when(attributesStorage.get(name)).thenReturn(attribute);

        Object retrievedAttribute = attributeClient.getAttribute(name);

        Assert.assertEquals(attribute, retrievedAttribute);
    }

    @Test
    public void setAttributesCallsSetOnStorage() {
        when(attributesValidator.isValid(any(Object.class))).thenReturn(true);

        attributeClient.setAttributes(testValues);

        verify(attributesStorage).set(testValues);
    }

    @Test
    public void setAttributesReturnsTrueIfAttributeValuesAreValid() {
        when(attributesValidator.isValid(any(Object.class))).thenReturn(true);

        boolean result = attributeClient.setAttributes(testValues);

        Assert.assertTrue(result);
    }

    @Test
    public void setAttributesDoesNotSaveAttributesInStorageIfAttributeValuesAreNotValid() {
        when(attributesValidator.isValid(any(Object.class))).thenReturn(false);

        attributeClient.setAttributes(testValues);

        Mockito.verifyNoInteractions(attributesStorage);
    }

    @Test
    public void setAttributesReturnsFalseIfAttributeValuesAreNotValid() {
        when(attributesValidator.isValid(any(Object.class))).thenReturn(false);

        boolean result = attributeClient.setAttributes(testValues);

        Assert.assertFalse(result);
    }

    @Test
    public void setAttributesLogsWarningMessageIfValueIsNotValid() {
        when(attributesValidator.isValid(any(Object.class))).thenReturn(false);

        attributeClient.setAttributes(testValues);

        verify(validationMessageLogger).w(eq("You passed an invalid attribute value for key1, acceptable types are String, double, float, long, int, boolean or Collections"), any());
    }

    @Test
    public void getAllAttributesFetchesValuesFromStorage() {
        when(attributesStorage.getAll()).thenReturn(testValues);

        Map<String, Object> allAttributes = attributeClient.getAllAttributes();

        Assert.assertEquals(testValues, allAttributes);
    }

    @Test
    public void clearAttributesCallsClearOnStorage() {

        attributeClient.clearAttributes();

        verify(attributesStorage).clear();
    }

    @Test
    public void removeCallsRemoveOnStorage() {

        attributeClient.removeAttribute("key");

        verify(attributesStorage).remove("key");
    }

    private Map<String, Object> getDefaultValues() {
        int[] array = new int[] { 1, 2, 3 };
        Map<String, Object> values = new HashMap<>();

        values.put("key1", 100);
        values.put("key2", "value2");
        values.put("key3", array);

        return values;
    }
}
