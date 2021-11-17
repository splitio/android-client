package io.split.android.client.service.attributes;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class AttributeClearTaskTest {

    @Mock
    PersistentAttributesStorage attributesStorage;
    private AttributeClearTask attributeClearTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        attributeClearTask = new AttributeClearTask(attributesStorage);
    }

    @Test
    public void executeCallsSetOnAttributesStorage() {
        attributeClearTask.execute();

        verify(attributesStorage).clear();
    }
}
