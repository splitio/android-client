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

public class UpdateAttributesInPersistentStorageTaskTest {

    @Mock
    PersistentAttributesStorage persistentAttributesStorage;
    @Mock
    AttributesStorage attributesStorage;
    private UpdateAttributesInPersistentStorageTask updateAttributesInPersistentStorageTask;
    private final Map<String, Object> testValues = new HashMap<>();
    private final String matchingKey = "user_key";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testValues.put("key1", "value1");
        testValues.put("key2", 100);
        when(attributesStorage.getAll()).thenReturn(testValues);
        updateAttributesInPersistentStorageTask = new UpdateAttributesInPersistentStorageTask(matchingKey, persistentAttributesStorage, attributesStorage);
    }

    @Test
    public void executeCallsSetOnAttributesStorage() {
        updateAttributesInPersistentStorageTask.execute();

        verify(persistentAttributesStorage).set(matchingKey, testValues);
    }

    @Test
    public void executeReadsAttributesLiveAtExecuteTimeNotAtConstructionTime() {
        Map<String, Object> updatedValues = new HashMap<>();
        updatedValues.put("key3", "value3");

        // change what getAll() returns AFTER construction but BEFORE execute()
        when(attributesStorage.getAll()).thenReturn(updatedValues);

        updateAttributesInPersistentStorageTask.execute();

        verify(persistentAttributesStorage).set(matchingKey, updatedValues);
    }
}
