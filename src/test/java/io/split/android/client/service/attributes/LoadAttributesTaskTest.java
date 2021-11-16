package io.split.android.client.service.attributes;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.storage.attributes.AttributesStorage;

public class LoadAttributesTaskTest {

    @Mock
    AttributesStorage attributesStorage;
    private LoadAttributesTask loadAttributesTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        loadAttributesTask = new LoadAttributesTask(attributesStorage);
    }

    @Test
    public void executeCallsClearOnAttributesStorage() {
        loadAttributesTask.execute();

        verify(attributesStorage).loadLocal();
    }
}