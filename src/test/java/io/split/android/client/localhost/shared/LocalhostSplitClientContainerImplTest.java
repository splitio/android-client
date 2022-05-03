package io.split.android.client.localhost.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesManagerFactory;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.EventsManagerCoordinator;
import io.split.android.client.localhost.LocalhostSplitFactory;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.engine.experiments.SplitParser;

public class LocalhostSplitClientContainerImplTest {

    @Mock
    private LocalhostSplitFactory mFactory;
    @Mock
    private SplitParser mSplitParser;
    @Mock
    private SplitsStorage mSplitsStorage;
    @Mock
    private EventsManagerCoordinator mEventsManagerCoordinator;
    @Mock
    private AttributesManagerFactory mAttributesManagerFactory;
    @Mock
    private AttributesMerger mAttributesMerger;
    @Mock
    private TelemetryStorageProducer mTelemetryStorageProducer;
    @Mock
    private SplitClientConfig mConfig;
    private LocalhostSplitClientContainerImpl mClientContainer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mAttributesManagerFactory.getManager(any(), any())).thenReturn(mock(AttributesManager.class));
        mClientContainer = getClientContainer();
    }

    @Test
    public void getClientForKeyReturnsSameInstance() {
        Key key = new Key("matching_key", "bucketing_key");

        SplitClient firstClient = mClientContainer.getClient(key);
        SplitClient secondClient = mClientContainer.getClient(key);

        assertEquals(firstClient, secondClient);
    }

    @Test
    public void getAllReturnsAllCreatedClients() {
        Key key = new Key("matching_key", "bucketing_key");
        Key secondKey = new Key("matching_key_2", "bucketing_key_2");

        SplitClient firstClient = mClientContainer.getClient(key);
        SplitClient secondClient = mClientContainer.getClient(secondKey);

        Collection<SplitClient> allClients = mClientContainer.getAll();

        assertEquals(2, allClients.size());
        assertTrue(allClients.contains(firstClient));
        assertTrue(allClients.contains(secondClient));
    }

    @Test
    public void gettingNewClientRegistersEventManager() {
        Key key = new Key("matching_key", "bucketing_key");

        mClientContainer.getClient(key);

        verify(mEventsManagerCoordinator).registerEventsManager(eq("matching_key"), eq("bucketing_key"), any());
    }

    @NonNull
    private LocalhostSplitClientContainerImpl getClientContainer() {
        return new LocalhostSplitClientContainerImpl(mFactory, mConfig, mSplitsStorage, mSplitParser, mAttributesManagerFactory, mAttributesMerger, mTelemetryStorageProducer, mEventsManagerCoordinator);
    }
}
