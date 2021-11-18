package io.split.android.client.service.attributes;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class LoadAttributesTaskTest {

    @Mock
    AttributesStorage attributesStorage;
    @Mock
    PersistentAttributesStorage persistentAttributesStorage;
    private LoadAttributesTask loadAttributesTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        loadAttributesTask = new LoadAttributesTask(attributesStorage, persistentAttributesStorage);
    }

    @Test
    public void executeSetsValuesFromPersistentStorageIntoMemoryStorage() {
        Map<String, Object> valuesInPersistentStorage = new HashMap<>();
        valuesInPersistentStorage.put("key1", "value1");
        valuesInPersistentStorage.put("key2", 200);
        when(persistentAttributesStorage.getAll()).thenReturn(valuesInPersistentStorage);

        SplitTaskExecutionInfo result = loadAttributesTask.execute();

        verify(persistentAttributesStorage).getAll();
        verify(attributesStorage).set(valuesInPersistentStorage);
        Assert.assertEquals(SplitTaskType.LOAD_LOCAL_ATTRIBUTES, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void executeDoesNotFetchValuesFromPersistentStorageIfItIsNull() {
        loadAttributesTask = new LoadAttributesTask(attributesStorage, null);

        SplitTaskExecutionInfo result = loadAttributesTask.execute();

        verifyNoInteractions(persistentAttributesStorage);
        verifyNoInteractions(attributesStorage);
        Assert.assertEquals(SplitTaskType.LOAD_LOCAL_ATTRIBUTES, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }
}