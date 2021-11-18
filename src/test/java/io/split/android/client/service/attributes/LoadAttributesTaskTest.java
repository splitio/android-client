package io.split.android.client.service.attributes;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

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

        loadAttributesTask.execute();

        verify(persistentAttributesStorage).getAll();
        verify(attributesStorage).set(valuesInPersistentStorage);
    }
}