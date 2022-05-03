package io.split.android.client.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitClientFactory;
import io.split.android.client.api.Key;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.mysegments.MySegmentsTaskFactoryProvider;
import io.split.android.client.service.sseclient.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.sseclient.PushNotificationManagerDeferredStartTask;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsBackgroundSyncScheduleTask;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsWorkManagerWrapper;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

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
    @Mock
    private SplitTaskExecutor mSplitTaskExecutor;
    @Mock
    private MySegmentsWorkManagerWrapper mWorkManagerWrapper;

    private final String mDefaultMatchingKey = "matching_key";
    private SplitClientContainer mClientContainer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mSplitApiFacade.getMySegmentsFetcher(any())).thenReturn(mock(HttpFetcher.class));
        when(mStorageContainer.getMySegmentsStorage(any())).thenReturn(mock(MySegmentsStorage.class));
        mClientContainer = getSplitClientContainer(mDefaultMatchingKey, true);
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

        SplitClientContainer container = getSplitClientContainer("default_key", true);

        container.getClient(defaultKey);

        verify(mSplitClientFactory).getClient(eq(defaultKey), any(), any(), eq(true));
    }

    @Test
    public void defaultClientIsNotRequestedWhenKeyIsNotDefault() {
        Key nonDefaultKey = new Key("non_default_key", "non_default_key");

        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(nonDefaultKey), any(), any(), anyBoolean())).thenReturn(clientMock);

        SplitClientContainer container = getSplitClientContainer(mDefaultMatchingKey, true);

        container.getClient(nonDefaultKey);

        verify(mSplitClientFactory).getClient(eq(nonDefaultKey), any(), any(), eq(false));
    }

    @Test
    public void pushNotificationManagerIsStartedWhenAddingNewKeyAndStreamingIsEnabled() {
        Key key = new Key("new_key");
        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), any(), any(), anyBoolean())).thenReturn(clientMock);

        mClientContainer.getClient(key);

        scheduleStreamingConnection();
    }

    @Test
    public void pushNotificationManagerIsStartedOnlyOnceWhenAddingMultipleClientsAndStreamingIsEnabled() {
        Key key = new Key("default_key");
        Key newKey = new Key("new_key");
        SplitClient clientMock = mock(SplitClient.class);
        SplitClient clientMock2 = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), any(), any(), anyBoolean())).thenReturn(clientMock);
        when(mSplitClientFactory.getClient(eq(newKey), any(), any(), anyBoolean())).thenReturn(clientMock2);

        mClientContainer.getClient(key);
        mClientContainer.getClient(newKey);

        scheduleStreamingConnection();
    }

    @Test
    public void pushNotificationManagerIsNotStartedWhenStreamingIsNotEnabled() {
        Key key = new Key("new_key");
        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), any(), any(), anyBoolean())).thenReturn(clientMock);

        SplitClientContainer container = getSplitClientContainer(mDefaultMatchingKey, false);

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

    @Test
    public void taskListenerSetsResultToFalse() {
        AtomicBoolean result = new AtomicBoolean(true);
        SplitClientContainerImpl.StreamingConnectionExecutionListener streamingConnectionExecutionListener = new SplitClientContainerImpl.StreamingConnectionExecutionListener(result);
        streamingConnectionExecutionListener.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));

        assertFalse(result.get());
    }

    @Test
    public void schedulingTaskListenerSetsResultToFalse() {
        AtomicBoolean result = new AtomicBoolean(true);
        SplitClientContainerImpl.WorkManagerSchedulingListener listener = new SplitClientContainerImpl.WorkManagerSchedulingListener(result);
        listener.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));

        assertFalse(result.get());
    }

    @Test
    public void jobsAreScheduledInWorkManagerAfterClientIsCreatedAndSyncInBackgroundIsOn() {
        Key key = new Key("matching_key");
        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), any(), any(), anyBoolean())).thenReturn(clientMock);
        when(mConfig.synchronizeInBackground()).thenReturn(true);

        mClientContainer.getClient(key);

        verify(mSplitTaskExecutor).schedule(argThat(argument -> argument instanceof MySegmentsBackgroundSyncScheduleTask), eq(5L), any());
    }

    @Test
    public void jobsAreNotScheduledInWorkManagerAfterClientIsCreatedAndSyncInBackgroundIsOff() {
        Key key = new Key("matching_key");
        SplitClient clientMock = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), any(), any(), anyBoolean())).thenReturn(clientMock);
        when(mConfig.synchronizeInBackground()).thenReturn(false);

        mClientContainer.getClient(key);

        verify(mSplitTaskExecutor, times(0)).schedule(argThat(argument -> argument instanceof MySegmentsBackgroundSyncScheduleTask), eq(5L), any());
        verify(mWorkManagerWrapper).removeWork();
    }

    @Test
    public void jobsAreScheduledOnceInWorkManagerAfterClientIsCreatedAndSyncInBackgroundIsOn() {
        Key key = new Key("default_key");
        Key newKey = new Key("new_key");
        SplitClient clientMock = mock(SplitClient.class);
        SplitClient clientMock2 = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), any(), any(), anyBoolean())).thenReturn(clientMock);
        when(mSplitClientFactory.getClient(eq(newKey), any(), any(), anyBoolean())).thenReturn(clientMock2);
        when(mConfig.synchronizeInBackground()).thenReturn(true);

        mClientContainer.getClient(key);
        mClientContainer.getClient(newKey);

        verify(mSplitTaskExecutor).schedule(argThat(argument -> argument instanceof MySegmentsBackgroundSyncScheduleTask), eq(5L), any());
    }

    @Test
    public void differentBucketingKeyDeliversNewClient() {
        Key key = new Key("default_key");
        Key keyWithBucketing = new Key("default_key", "bucketing_key");
        SplitClient clientMock = mock(SplitClient.class);
        SplitClient clientMock2 = mock(SplitClient.class);
        when(mSplitClientFactory.getClient(eq(key), any(), any(), anyBoolean())).thenReturn(clientMock);
        when(mSplitClientFactory.getClient(eq(keyWithBucketing), any(), any(), anyBoolean())).thenReturn(clientMock2);

        SplitClient client = mClientContainer.getClient(key);
        SplitClient client2 = mClientContainer.getClient(keyWithBucketing);

        assertNotEquals(client, client2);
    }

    @NonNull
    private SplitClientContainerImpl getSplitClientContainer(String mDefaultMatchingKey, boolean b) {
        return new SplitClientContainerImpl(
                mDefaultMatchingKey,
                mPushNotificationManager,
                b,
                mMySegmentsTaskFactoryProvider,
                mSplitApiFacade,
                mStorageContainer,
                mSplitTaskExecutor,
                mConfig,
                mSplitClientFactory,
                mClientComponentsRegister,
                mWorkManagerWrapper
        );
    }

    private void scheduleStreamingConnection() {
        verify(mSplitTaskExecutor).schedule(
                ArgumentMatchers.argThat((ArgumentMatcher<PushNotificationManagerDeferredStartTask>) Objects::nonNull),
                eq(5L),
                any());
    }
}
