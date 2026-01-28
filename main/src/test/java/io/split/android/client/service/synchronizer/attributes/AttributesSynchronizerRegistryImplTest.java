package io.split.android.client.service.synchronizer.attributes;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

public class AttributesSynchronizerRegistryImplTest {

    private AttributesSynchronizerRegistryImpl mRegistry;

    @Before
    public void setUp() {
        mRegistry = new AttributesSynchronizerRegistryImpl();
    }

    @Test
    public void loadAttributesFromCacheTriggersRegisteredSynchronizers() {
        AttributesSynchronizer syncMock = mock(AttributesSynchronizer.class);
        AttributesSynchronizer syncMock2 = mock(AttributesSynchronizer.class);
        mRegistry.registerAttributesSynchronizer("key", syncMock);
        mRegistry.registerAttributesSynchronizer("key2", syncMock2);

        mRegistry.loadAttributesFromCache();

        verify(syncMock).loadAttributesFromCache();
        verify(syncMock2).loadAttributesFromCache();
    }

    @Test
    public void loadAttributesFromCacheIsTriggeredForNewlyRegisteredSyncIfNecessary() {
        AttributesSynchronizer syncMock = mock(AttributesSynchronizer.class);
        AttributesSynchronizer syncMock2 = mock(AttributesSynchronizer.class);
        mRegistry.registerAttributesSynchronizer("key", syncMock);

        mRegistry.loadAttributesFromCache();

        mRegistry.registerAttributesSynchronizer("key2", syncMock2);
        verify(syncMock2).loadAttributesFromCache();
    }
}
