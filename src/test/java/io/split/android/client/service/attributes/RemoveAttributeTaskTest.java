package io.split.android.client.service.attributes;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.storage.attributes.AttributesStorage;

public class RemoveAttributeTaskTest {

    @Mock
    AttributesStorage attributesStorage;
    private String attributeName = "key";
    private RemoveAttributeTask removeAttributeTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        removeAttributeTask = new RemoveAttributeTask(attributesStorage, attributeName);
    }

    @Test
    public void executeCallsRemoveOnAttributesStorageWithCorrectKey() {
        removeAttributeTask.execute();

        verify(attributesStorage).remove(attributeName);
    }
}