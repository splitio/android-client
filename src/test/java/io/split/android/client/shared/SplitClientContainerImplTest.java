package io.split.android.client.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientFactory;
import io.split.android.client.api.Key;

public class SplitClientContainerImplTest {

    @Mock
    private SplitClientFactory mSplitClientFactory;
    private SplitClientContainer mClientContainer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mClientContainer = new SplitClientContainerImpl(mSplitClientFactory);
    }

    @Test
    public void getClientForKeyReturnsSameInstance() {
        Key key = new Key("matching_key", "bucketing_key");
        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(key, true)).thenReturn(clientMock);

        SplitClient firstClient = mClientContainer.getClient(key);
        SplitClient secondClient = mClientContainer.getClient(key);

        assertEquals(firstClient, secondClient);
    }

    @Test
    public void getAllReturnsAllCreatedClients() {
        Key key = new Key("matching_key", "bucketing_key");
        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(key, true)).thenReturn(clientMock);

        Key secondKey = new Key("matching_key_2", "bucketing_key_2");
        SplitClient clientMock2 = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(secondKey, false)).thenReturn(clientMock2);

        SplitClient firstClient = mClientContainer.getClient(key);
        SplitClient secondClient = mClientContainer.getClient(secondKey);

        Collection<SplitClient> allClients = mClientContainer.getAll();

        assertEquals(2, allClients.size());
        assertTrue(allClients.contains(firstClient));
        assertTrue(allClients.contains(secondClient));
    }
}
