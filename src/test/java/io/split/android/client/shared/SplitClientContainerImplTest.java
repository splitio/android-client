package io.split.android.client.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitClientFactory;
import io.split.android.client.SplitFactoryImpl;
import io.split.android.client.api.Key;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.mysegments.MySegmentsTaskFactoryProvider;
import io.split.android.client.service.sseclient.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticator;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.ValidationMessageLogger;

public class SplitClientContainerImplTest {

    @Mock
    private SplitClientConfig mConfig;
    @Mock
    private SplitStorageContainer mStorageContainer;
    @Mock
    private SplitApiFacade mSplitApiFacade;
    @Mock
    private SplitClientFactory mSplitClientFactory;
    @Mock
    private MySegmentsTaskFactoryProvider mMySegmentsTaskFactoryProvider;
    @Mock
    private PushNotificationManager mPushNotificationManager;
    @Mock
    private ClientComponentsRegister mClientComponentsRegister;
    private final String mDefaultMatchingKey = "matching_key";
    private SplitClientContainer mClientContainer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mSplitApiFacade.getMySegmentsFetcher(any())).thenReturn(mock(HttpFetcher.class));
        when(mStorageContainer.getMySegmentsStorage(any())).thenReturn(mock(MySegmentsStorage.class));
        mClientContainer = new SplitClientContainerImpl(
                mDefaultMatchingKey,
                mPushNotificationManager,
                true,
                mMySegmentsTaskFactoryProvider,
                mSplitApiFacade,
                mStorageContainer,
                mConfig,
                mSplitClientFactory,
                mClientComponentsRegister
        );
    }

    @Test
    public void getClientForKeyReturnsSameInstance() {
        Key key = new Key("matching_key", "bucketing_key");

        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), any(), any(), eq(true))).thenReturn(clientMock);

        SplitClient firstClient = mClientContainer.getClient(key);
        SplitClient secondClient = mClientContainer.getClient(key);

        assertEquals(firstClient, secondClient);
    }

    @Test
    public void getAllReturnsAllCreatedClients() {
        Key key = new Key("matching_key", "bucketing_key");
        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), any(), any(), eq(true))).thenReturn(clientMock);

        Key secondKey = new Key("matching_key_2", "bucketing_key_2");
        SplitClient clientMock2 = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(secondKey), any(), any(), eq(false))).thenReturn(clientMock2);

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
        when(mSplitClientFactory.getClient(eq(defaultKey), any(), any(), anyBoolean())).thenReturn(clientMock);

        SplitClientContainer container = new SplitClientContainerImpl(
                "default_key",
                mPushNotificationManager,
                true,
                mMySegmentsTaskFactoryProvider,
                mSplitApiFacade,
                mStorageContainer,
                mConfig,
                mSplitClientFactory,
                mClientComponentsRegister
        );

        container.getClient(defaultKey);

        verify(mSplitClientFactory).getClient(eq(defaultKey), any(), any(), eq(true));
    }

    @Test
    public void defaultClientIsNotRequestedWhenKeyIsNotDefault() {
        Key nonDefaultKey = new Key("non_default_key", "non_default_key");

        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(nonDefaultKey), any(), any(), anyBoolean())).thenReturn(clientMock);

        SplitClientContainer container = new SplitClientContainerImpl(
                mDefaultMatchingKey,
                mPushNotificationManager,
                true,
                mMySegmentsTaskFactoryProvider,
                mSplitApiFacade,
                mStorageContainer,
                mConfig,
                mSplitClientFactory,
                mClientComponentsRegister
        );

        container.getClient(nonDefaultKey);

        verify(mSplitClientFactory).getClient(eq(nonDefaultKey), any(), any(), eq(false));
    }

    @Test
    public void pushNotificationManagerIsStartedWhenAddingNewKeyAndStreamingIsEnabled() {
        Key key = new Key("new_key");
        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), any(), any(), anyBoolean())).thenReturn(clientMock);

        mClientContainer.getClient(key);

        verify(mPushNotificationManager).start();
    }

    @Test
    public void pushNotificationManagerIsReStartedWhenAddingNewKeyAndStreamingIsEnabled() {
        Key key = new Key("default_key");
        Key newKey = new Key("new_key");
        SplitClient clientMock = mock(SplitClient.class);
        SplitClient clientMock2 = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), any(), any(), anyBoolean())).thenReturn(clientMock);
        when(mSplitClientFactory.getClient(eq(newKey), any(), any(), anyBoolean())).thenReturn(clientMock2);

        mClientContainer.getClient(key);
        mClientContainer.getClient(newKey);

        verify(mPushNotificationManager, times(2)).start();
    }

    @Test
    public void pushNotificationManagerIsNotStartedWhenStreamingIsNotEnabled() {
        Key key = new Key("new_key");
        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), any(), any(), anyBoolean())).thenReturn(clientMock);

        SplitClientContainer container = new SplitClientContainerImpl(
                mDefaultMatchingKey,
                mPushNotificationManager,
                false,
                mMySegmentsTaskFactoryProvider,
                mSplitApiFacade,
                mStorageContainer,
                mConfig,
                mSplitClientFactory,
                mClientComponentsRegister
        );
        container.getClient(key);

        verifyNoInteractions(mPushNotificationManager);
    }

    @Test
    public void componentsAreRegisteredWhenCreatingAClient() {
        Key key = new Key("matching_key");
        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), any(), any(), anyBoolean())).thenReturn(clientMock);
        mClientContainer.getClient(key);

        verify(mClientComponentsRegister).registerComponents(eq(key), any(), any());
    }

    @Test
    public void callingRemoveUnregistersComponentsForKey() {
        mClientContainer.remove("matching_key");
        verify(mClientComponentsRegister).unregisterComponentsForKey("matching_key");
    }
}
