package io.split.android.client.service.attributes;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class ClearAttributesInPersistentStorageTaskTest {

    @Mock
    PersistentAttributesStorage attributesStorage;
    private ClearAttributesInPersistentStorageTask clearAttributesInPersistentStorageTask;
    private String matchingKey = "user_key";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        clearAttributesInPersistentStorageTask = new ClearAttributesInPersistentStorageTask(matchingKey, attributesStorage);
    }

    @Test
    public void executeCallsSetOnAttributesStorage() {
        clearAttributesInPersistentStorageTask.execute();

        verify(attributesStorage).clear(matchingKey);
    }
}
