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
import java.util.HashSet;

import io.split.android.client.FlagSetsFilter;
import io.split.android.client.FlagSetsFilterImpl;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesManagerFactory;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.EventsManagerCoordinator;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.events.executors.SplitEventExecutorResources;
import io.split.android.client.localhost.LocalhostSplitFactory;
import io.split.android.client.service.executor.SplitTaskExecutor;
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
    @Mock
    private SplitTaskExecutor mTaskExecutor;
    private FlagSetsFilter mFlagSetsFilter;
    private LocalhostSplitClientContainerImpl mClientContainer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mAttributesManagerFactory.getManager(any(), any())).thenReturn(mock(AttributesManager.class));
        mFlagSetsFilter = new FlagSetsFilterImpl(new HashSet<>());
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

        verify(mEventsManagerCoordinator).registerEventsManager(eq(key), any());
    }

    @Test
    public void gettingNewClientNotifiesInternalEvents() {
        // Create a mocked SplitEventsManager
        SplitEventsManager mockEventsManager = mock(SplitEventsManager.class);
        SplitEventExecutorResources mockExecutorResources = mock(SplitEventExecutorResources.class);
        when(mockEventsManager.getExecutorResources()).thenReturn(mockExecutorResources);

        // Create a mocked factory that returns the mocked events manager
        SplitEventsManagerFactory mockFactory = () -> mockEventsManager;

        // Create client container with the mocked factory using @VisibleForTesting constructor
        LocalhostSplitClientContainerImpl clientContainer = new LocalhostSplitClientContainerImpl(
                mFactory,
                mConfig,
                mSplitsStorage,
                mSplitParser,
                mAttributesManagerFactory,
                mAttributesMerger,
                mTelemetryStorageProducer,
                mEventsManagerCoordinator,
                mTaskExecutor,
                mFlagSetsFilter,
                mockFactory
        );

        Key key = new Key("matching_key", "bucketing_key");
        clientContainer.getClient(key);

        // Verify that notifyInternalEvent is called on the mocked events manager
        verify(mockEventsManager).notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        verify(mockEventsManager).notifyInternalEvent(SplitInternalEvent.MEMBERSHIPS_SYNC_COMPLETE);
        verify(mockEventsManager).notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);
    }

    @NonNull
    private LocalhostSplitClientContainerImpl getClientContainer() {
        return new LocalhostSplitClientContainerImpl(mFactory,
                mConfig,
                mSplitsStorage,
                mSplitParser,
                mAttributesManagerFactory,
                mAttributesMerger,
                mTelemetryStorageProducer,
                mEventsManagerCoordinator,
                mTaskExecutor,
                mFlagSetsFilter);
    }
}
