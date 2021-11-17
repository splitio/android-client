package io.split.android.client.service.attributes;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class AttributeUpdateTaskTest {

    @Mock
    PersistentAttributesStorage attributesStorage;
    private AttributeUpdateTask attributeUpdateTask;
    private final Map<String, Object> testValues = new HashMap<>();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testValues.put("key1", "value1");
        testValues.put("key2", 100);
        attributeUpdateTask = new AttributeUpdateTask(attributesStorage, testValues);
    }

    @Test
    public void executeCallsSetOnAttributesStorage() {
        attributeUpdateTask.execute();

        verify(attributesStorage).set(testValues);
    }
}
