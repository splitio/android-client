package io.split.android.client.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientFactory;
import io.split.android.client.api.Key;
import io.split.android.client.service.sseclient.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticator;

public class SplitClientContainerImplTest {

    @Mock
    private SplitClientFactory mSplitClientFactory;
    @Mock
    private SseAuthenticator mSseAuthenticator;
    @Mock
    private PushNotificationManager mPushNotificationManager;
    private SplitClientContainer mClientContainer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mClientContainer = new SplitClientContainerImpl(true, "matching_key", mSplitClientFactory, mSseAuthenticator, mPushNotificationManager);
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

    @Test
    public void defaultClientIsCorrectlyRequested() {
        Key defaultKey = new Key("default_key", "default_key");

        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(defaultKey), anyBoolean())).thenReturn(clientMock);

        SplitClientContainer container = new SplitClientContainerImpl(true, "default_key",
                mSplitClientFactory, mSseAuthenticator, mPushNotificationManager);

        container.getClient(defaultKey);

        verify(mSplitClientFactory).getClient(defaultKey, true);
    }

    @Test
    public void defaultClientIsNotRequestedWhenKeyIsNotDefault() {
        Key nonDefaultKey = new Key("non_default_key", "non_default_key");

        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(nonDefaultKey), anyBoolean())).thenReturn(clientMock);

        SplitClientContainer container = new SplitClientContainerImpl(true, "default_key",
                mSplitClientFactory, mSseAuthenticator, mPushNotificationManager);

        container.getClient(nonDefaultKey);

        verify(mSplitClientFactory).getClient(nonDefaultKey, false);
    }

    @Test
    public void pushNotificationManagerIsStartedWhenAddingNewKeyAndStreamingIsEnabled() {
        Key key = new Key("new_key");
        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), anyBoolean())).thenReturn(clientMock);

        mClientContainer.getClient(key);

        verify(mPushNotificationManager).start();
    }

    @Test
    public void pushNotificationManagerIsNotStartedWhenStreamingIsNotEnabled() {
        Key key = new Key("new_key");
        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), anyBoolean())).thenReturn(clientMock);

        SplitClientContainer container = new SplitClientContainerImpl(false, key.matchingKey(), mSplitClientFactory, mSseAuthenticator, mPushNotificationManager);
        container.getClient(key);

        verifyNoInteractions(mPushNotificationManager);
    }
}
