package io.split.android.client.events.executors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import io.split.android.client.SplitClient;

public class SplitEventExecutorResourcesImplTest {

    @Test
    public void clientInstanceIsKept() {

        SplitEventExecutorResourcesImpl splitEventExecutorResources = new SplitEventExecutorResourcesImpl();
        SplitClient client = mock(SplitClient.class);
        splitEventExecutorResources.setSplitClient(client);
        assertEquals(client, splitEventExecutorResources.getSplitClient());
    }
}
