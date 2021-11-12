package io.split.android.client.attributes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.validators.AttributesValidator;

public class AttributesClientImplTest {

    @Mock
    AttributesStorage attributesStorage;
    @Mock
    AttributesValidator attributesValidator;
    @Mock
    SplitTaskFactory splitTaskFactory;
    @Mock
    SplitTaskExecutor splitTaskExecutor;
    private AttributesClientImpl attributeClient;
    private Map<String, Object> testValues;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        attributeClient = new AttributesClientImpl(attributesStorage, attributesValidator, splitTaskFactory, splitTaskExecutor);
        testValues = getDefaultValues();
    }

    @Test
    public void setAttributeSavesAttributeInStorageIfAttributeValueIsValid() {
        String name = "key";
        String attribute = "value";
        Mockito.when(attributesValidator.isValid(attribute)).thenReturn(true);

        attributeClient.setAttribute(name, attribute);

        Mockito.verify(attributesStorage).set(name, attribute);
    }

    @Test
    public void setAttributeReturnsTrueIfAttributeValueIsValid() {
        String name = "key";
        String attribute = "value";
        Mockito.when(attributesValidator.isValid(attribute)).thenReturn(true);

        boolean result = attributeClient.setAttribute(name, attribute);

        Assert.assertTrue(result);
    }

    @Test
    public void setAttributeDoesNotSaveAttributeInStorageIfAttributeValueIsNotValid() {
        String name = "key";
        String attribute = "value";
        Mockito.when(attributesValidator.isValid(attribute)).thenReturn(false);

        attributeClient.setAttribute(name, attribute);

        Mockito.verifyNoInteractions(attributesStorage);
    }

    @Test
    public void setAttributeReturnsFalseIfAttributeValueNotIsValid() {
        String name = "key";
        String attribute = "value";
        Mockito.when(attributesValidator.isValid(attribute)).thenReturn(false);

        boolean result = attributeClient.setAttribute(name, attribute);

        Assert.assertFalse(result);
    }

    @Test
    public void getReturnsValueFetchedFromStorage() {
        String name = "key";
        int attribute = 100;
        Mockito.when(attributesStorage.get(name)).thenReturn(attribute);

        Object retrievedAttribute = attributeClient.getAttribute(name);

        Assert.assertEquals(attribute, retrievedAttribute);
    }

    @Test
    public void setAttributesSavesAttributesInStorageIfAttributeValuesAreValid() {
        Mockito.when(attributesValidator.isValid(testValues.values())).thenReturn(true);

        attributeClient.setAttributes(testValues);

        Mockito.verify(attributesStorage).set(testValues);
    }

    @Test
    public void setAttributesReturnsTrueIfAttributeValuesAreValid() {
        Mockito.when(attributesValidator.isValid(testValues.values())).thenReturn(true);

        boolean result = attributeClient.setAttributes(testValues);

        Assert.assertTrue(result);
    }

    @Test
    public void setAttributesDoesNotSaveAttributesInStorageIfAttributeValuesAreNotValid() {
        Mockito.when(attributesValidator.isValid(testValues.values())).thenReturn(false);

        attributeClient.setAttributes(testValues);

        Mockito.verifyNoInteractions(attributesStorage);
    }

    @Test
    public void setAttributesReturnsFalseIfAttributeValuesAreNotValid() {
        Mockito.when(attributesValidator.isValid(testValues.values())).thenReturn(false);

        boolean result = attributeClient.setAttributes(testValues);

        Assert.assertFalse(result);
    }

    @Test
    public void getAllAttributesFetchesValuesFromStorage() {
        Mockito.when(attributesStorage.getAll()).thenReturn(testValues);

        Map<String, Object> allAttributes = attributeClient.getAllAttributes();

        Assert.assertEquals(testValues, allAttributes);
    }

    @Test
    public void clearAttributesCallsClearOnStorage() {

        attributeClient.clearAttributes();

        Mockito.verify(attributesStorage).clear();
    }

    @Test
    public void removeCallsRemoveOnStorage() {

        attributeClient.removeAttribute("key");

        Mockito.verify(attributesStorage).remove("key");
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
