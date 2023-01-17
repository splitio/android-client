package io.split.android.client.localhost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.split.android.client.localhost.LocalhostSplitFactoryTestBuilder.getFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.split.android.client.SplitClient;
import io.split.android.client.api.Key;
import io.split.android.client.localhost.shared.LocalhostSplitClientContainerImpl;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.engine.experiments.SplitParser;

public class LocalhostSplitFactoryTest {

    @Mock
    private SplitParser mSplitParser;
    @Mock
    private SplitsStorage mSplitStorage;
    @Mock
    private LocalhostSplitClientContainerImpl mLocalhostSplitClientContainer;
    @Mock
    private LocalhostSynchronizer mSynchronizer;

    private LocalhostSplitFactory mFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        String mDefaultKey = "matching_key";
        mFactory = getFactory(mSplitStorage, mSplitParser, mDefaultKey, mSynchronizer, mLocalhostSplitClientContainer);
    }

    @Test
    public void clientFetchesDefaultKeyClientFromContainer() {
        ArgumentCaptor<Key> argumentCaptor = ArgumentCaptor.forClass(Key.class);

        mFactory.client();

        verify(mLocalhostSplitClientContainer).getClient(argumentCaptor.capture());

        assertEquals("matching_key", argumentCaptor.getValue().matchingKey());
    }

    @Test
    public void clientWithKeyFetchesCorrectClientFromContainer() {
        ArgumentCaptor<Key> argumentCaptor = ArgumentCaptor.forClass(Key.class);

        mFactory.client(new Key("new_key"));

        verify(mLocalhostSplitClientContainer).getClient(argumentCaptor.capture());

        assertEquals("new_key", argumentCaptor.getValue().matchingKey());
    }

    @Test
    public void managerIsNotNull() {
        assertNotNull(mFactory.manager());
    }

    @Test
    public void flushCallsFlushOnAllClients() {
        Set<SplitClient> clients = new HashSet<>(Arrays.asList(mock(SplitClient.class), mock(SplitClient.class)));
        when(mLocalhostSplitClientContainer.getAll()).thenReturn(clients);

        mFactory.flush();

        verify(mLocalhostSplitClientContainer).getAll();
        for (SplitClient client : clients) {
            verify(client).flush();
        }
    }
}
