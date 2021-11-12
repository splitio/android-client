package io.split.android.client.service.attributes;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.storage.attributes.AttributesStorage;

public class UpdateSingleAttributeTaskTest {

    @Mock
    AttributesStorage attributesStorage;
    private final String attributeName = "key";
    private final String attributeValue = "value";
    private UpdateSingleAttributeTask updateSingleAttributeTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        updateSingleAttributeTask = new UpdateSingleAttributeTask(attributesStorage, attributeName, attributeValue);
    }

    @Test
    public void executeCallsSetAttributesOnAttributesStorage() {
        updateSingleAttributeTask.execute();

        verify(attributesStorage).set(attributeName, attributeValue);
    }
}