package io.split.android.client.service.attributes;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.storage.attributes.AttributesStorage;

public class UpdateAttributesTaskTest {

    @Mock
    AttributesStorage attributesStorage;
    private Map<String, Object> attributes;
    private UpdateAttributesTask updateAttributesTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        attributes = new HashMap<>();
        attributes.put("key", "value");
        attributes.put("key2", 100);
        updateAttributesTask = new UpdateAttributesTask(attributesStorage, attributes);
    }

    @Test
    public void executeCallsSetAttributesOnAttributesStorage() {
        updateAttributesTask.execute();

        verify(attributesStorage).set(attributes);
    }

}