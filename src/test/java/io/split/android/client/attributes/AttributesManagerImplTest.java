package io.split.android.client.attributes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

import io.split.android.client.service.attributes.AttributeClearTask;
import io.split.android.client.service.attributes.AttributeTaskFactory;
import io.split.android.client.service.attributes.AttributeUpdateTask;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.validators.AttributesValidator;
import io.split.android.client.validators.ValidationMessageLogger;

public class AttributesManagerImplTest {

    @Mock
    AttributesStorage attributesStorage;
    @Mock
    AttributesValidator attributesValidator;
    @Mock
    ValidationMessageLogger validationMessageLogger;
    @Mock
    PersistentAttributesStorage persistentAttributesStorage;
    @Mock
    AttributeTaskFactory attributeTaskFactory;
    @Mock
    SplitTaskExecutor splitTaskExecutor;

    private AttributesManagerImpl attributeClient;
    private Map<String, Object> testValues;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        attributeClient = new AttributesManagerImpl(attributesStorage,
                attributesValidator,
                validationMessageLogger,
                persistentAttributesStorage,
                attributeTaskFactory,
                splitTaskExecutor);
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
    public void setAttributeLaunchesAttributeUpdateTaskIfValueIsValid() {
        String name = "key";
        String attribute = "value";
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(name, attribute);

        AttributeUpdateTask attributeUpdateTask = mock(AttributeUpdateTask.class);
        when(attributesStorage.getAll()).thenReturn(attributeMap);
        when(attributesValidator.isValid(attribute)).thenReturn(true);
        when(attributeTaskFactory.createAttributeUpdateTask(persistentAttributesStorage, attributeMap)).thenReturn(attributeUpdateTask);

        attributeClient.setAttribute(name, attribute);

        verify(attributeTaskFactory).createAttributeUpdateTask(persistentAttributesStorage, attributeMap);
        verify(splitTaskExecutor).submit(attributeUpdateTask, null);
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
    public void setAttributesLaunchesAttributeUpdateTaskIfValuesAreValid() {
        String name = "key";
        String attribute = "value";
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(name, attribute);

        AttributeUpdateTask attributeUpdateTask = mock(AttributeUpdateTask.class);
        when(attributesStorage.getAll()).thenReturn(attributeMap);
        when(attributesValidator.isValid(attribute)).thenReturn(true);
        when(attributeTaskFactory.createAttributeUpdateTask(persistentAttributesStorage, attributeMap)).thenReturn(attributeUpdateTask);

        attributeClient.setAttributes(attributeMap);

        verify(attributeTaskFactory).createAttributeUpdateTask(persistentAttributesStorage, attributeMap);
        verify(splitTaskExecutor).submit(attributeUpdateTask, null);
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
    public void clearLaunchesAttributeClearTask() {
        AttributeClearTask attributeClearTask = mock(AttributeClearTask.class);
        when(attributeTaskFactory.createAttributeClearTask(persistentAttributesStorage)).thenReturn(attributeClearTask);

        attributeClient.clearAttributes();

        verify(attributeTaskFactory).createAttributeClearTask(persistentAttributesStorage);
        verify(splitTaskExecutor).submit(attributeClearTask, null);
    }

    @Test
    public void removeCallsRemoveOnStorage() {

        attributeClient.removeAttribute("key");

        verify(attributesStorage).remove("key");
    }

    @Test
    public void removeLaunchesAttributeUpdateTask() {
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("key", "value");
        attributeMap.put("key2", 100);

        AttributeUpdateTask attributeUpdateTask = mock(AttributeUpdateTask.class);
        when(attributesStorage.getAll()).thenReturn(attributeMap);
        when(attributesValidator.isValid(any())).thenReturn(true);
        when(attributeTaskFactory.createAttributeUpdateTask(persistentAttributesStorage, attributeMap)).thenReturn(attributeUpdateTask);

        attributeClient.removeAttribute("key");

        verify(attributeTaskFactory).createAttributeUpdateTask(persistentAttributesStorage, attributeMap);
        verify(splitTaskExecutor).submit(attributeUpdateTask, null);
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
