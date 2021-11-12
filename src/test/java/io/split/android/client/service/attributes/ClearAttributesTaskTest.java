package io.split.android.client.service.attributes;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.storage.attributes.AttributesStorage;

public class ClearAttributesTaskTest {

    @Mock
    AttributesStorage attributesStorage;
    private ClearAttributesTask clearAttributesTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        clearAttributesTask = new ClearAttributesTask(attributesStorage);
    }

    @Test
    public void executeCallsClearOnAttributesStorage() {
        clearAttributesTask.execute();

        verify(attributesStorage).clear();
    }
}