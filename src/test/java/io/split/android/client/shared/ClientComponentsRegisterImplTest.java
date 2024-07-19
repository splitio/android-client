package io.split.android.client.shared;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.api.Key;
import io.split.android.client.events.EventsManagerRegistry;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessor;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessorFactory;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessorRegistry;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorkerRegistry;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticator;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerFactory;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegistry;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizer;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerFactory;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistry;
import io.split.android.client.storage.common.SplitStorageContainer;

public class ClientComponentsRegisterImplTest {

    @Mock
    private MySegmentsSynchronizerFactory mMySegmentsSynchronizerFactory;
    @Mock
    private SplitStorageContainer mStorageContainer;
    @Mock
    private AttributesSynchronizerFactory mAttributesSynchronizerFactory;
    @Mock
    private AttributesSynchronizerRegistry mAttributesSynchronizerRegistry;
    @Mock
    private MySegmentsSynchronizerRegistry mMySegmentsSynchronizerRegistry;
    @Mock
    private MySegmentsUpdateWorkerRegistry mMySegmentsUpdateWorkerRegistry;
    @Mock
    private EventsManagerRegistry mEventsManagerRegistry;
    @Mock
    private SseAuthenticator mSseAuthenticator;
    @Mock
    private MySegmentsNotificationProcessorRegistry mMySegmentsNotificationProcessorRegistry;
    @Mock
    private MySegmentsNotificationProcessorFactory mMySegmentsNotificationProcessorFactory;
    @Mock
    private MySegmentsV2PayloadDecoder mMySegmentsV2PayloadDecoder;

    @Mock
    private MySegmentsTaskFactory mMySegmentsTaskFactory;
    @Mock
    private MySegmentsTaskFactory mMyLargeSegmentsTaskFactory;
    @Mock
    private SplitEventsManager mSplitEventsManager;
    @Mock
    private MySegmentsSynchronizer mMySegmentsSynchronizer;
    @Mock
    private MySegmentsSynchronizer mMyLargeSegmentsSynchronizer;

    private final Key mMatchingKey = new Key("matching_key", "bucketing_key");

    private ClientComponentsRegisterImpl register;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mMySegmentsSynchronizerFactory.getSynchronizer(mMySegmentsTaskFactory, mSplitEventsManager, SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE, 1800))
                .thenReturn(mMySegmentsSynchronizer);
        when(mMySegmentsSynchronizerFactory.getSynchronizer(mMyLargeSegmentsTaskFactory, mSplitEventsManager, SplitInternalEvent.MY_LARGE_SEGMENTS_LOADED_FROM_STORAGE, 60))
                .thenReturn(mMyLargeSegmentsSynchronizer);

        register = getRegister(false);
    }

    @Test
    public void attributesSynchronizerIsRegistered() {
        register.registerComponents(mMatchingKey, mSplitEventsManager, mMySegmentsTaskFactory, mMyLargeSegmentsTaskFactory);

        verify(mAttributesSynchronizerRegistry).registerAttributesSynchronizer(eq("matching_key"), any());
    }

    @Test
    public void mySegmentsSynchronizerIsRegistered() {
        register.registerComponents(mMatchingKey, mSplitEventsManager, mMySegmentsTaskFactory, mMyLargeSegmentsTaskFactory);

        verify(mMySegmentsSynchronizerRegistry).registerMySegmentsSynchronizer("matching_key", mMySegmentsSynchronizer);
    }

    @Test
    public void mySegmentsUpdateWorkerIsRegistered() {
        register.registerComponents(mMatchingKey, mSplitEventsManager, mMySegmentsTaskFactory, mMyLargeSegmentsTaskFactory);

        verify(mMySegmentsUpdateWorkerRegistry).registerMySegmentsUpdateWorker(eq("matching_key"), any());
    }

    @Test
    public void mySegmentsNotificationProcessorIsRegistered() {
        register.registerComponents(mMatchingKey, mSplitEventsManager, mMySegmentsTaskFactory, mMyLargeSegmentsTaskFactory);

        verify(mMySegmentsNotificationProcessorRegistry).registerMySegmentsProcessor(eq("matching_key"), (MySegmentsNotificationProcessor) any());
    }

    @Test
    public void eventsManagerIsRegistered() {
        register.registerComponents(mMatchingKey, mSplitEventsManager, mMySegmentsTaskFactory, mMyLargeSegmentsTaskFactory);

        verify(mEventsManagerRegistry).registerEventsManager(mMatchingKey, mSplitEventsManager);
    }

    @Test
    public void myLargeSegmentsSynchronizerIsNotRegisteredWhenLargeSegmentsIsNotEnabled() {
        register.registerComponents(mMatchingKey, mSplitEventsManager, mMySegmentsTaskFactory, mMyLargeSegmentsTaskFactory);

        verify(mMySegmentsSynchronizerFactory, times(0)).getSynchronizer(mMyLargeSegmentsTaskFactory, mSplitEventsManager, SplitInternalEvent.MY_LARGE_SEGMENTS_LOADED_FROM_STORAGE, 60);
        verify(mMySegmentsSynchronizerRegistry, times(0)).registerMyLargeSegmentsSynchronizer("matching_key", mMySegmentsSynchronizer);
    }

    @Test
    public void myLargeSegmentsSynchronizerIsNotRegisteredWhenTaskFactoryIsNull() {
        register = getRegister(true);
        register.registerComponents(mMatchingKey, mSplitEventsManager, mMySegmentsTaskFactory, null);

        verify(mMySegmentsSynchronizerFactory, times(0)).getSynchronizer(mMyLargeSegmentsTaskFactory, mSplitEventsManager, SplitInternalEvent.MY_LARGE_SEGMENTS_LOADED_FROM_STORAGE, 60);
        verify(mMySegmentsSynchronizerRegistry, times(0)).registerMyLargeSegmentsSynchronizer("matching_key", mMySegmentsSynchronizer);
    }

    @Test
    public void myLargeSegmentsSynchronizerIsRegisteredWhenLargeSegmentsIsEnabledAndTaskFactoryIsNotNull() {
        register = getRegister(true);
        register.registerComponents(mMatchingKey, mSplitEventsManager, mMySegmentsTaskFactory, mMyLargeSegmentsTaskFactory);

        verify(mMySegmentsSynchronizerFactory).getSynchronizer(mMyLargeSegmentsTaskFactory, mSplitEventsManager, SplitInternalEvent.MY_LARGE_SEGMENTS_LOADED_FROM_STORAGE, 60);
        verify(mMySegmentsSynchronizerRegistry).registerMyLargeSegmentsSynchronizer("matching_key", mMyLargeSegmentsSynchronizer);
    }

    @Test
    public void componentsAreCorrectlyUnregistered() {
        register.unregisterComponentsForKey(mMatchingKey);

        verify(mAttributesSynchronizerRegistry).unregisterAttributesSynchronizer("matching_key");
        verify(mMySegmentsSynchronizerRegistry).unregisterMySegmentsSynchronizer("matching_key");
        verify(mMySegmentsUpdateWorkerRegistry).unregisterMySegmentsUpdateWorker("matching_key");
        verify(mMySegmentsNotificationProcessorRegistry).unregisterMySegmentsProcessor("matching_key");
        verify(mEventsManagerRegistry).unregisterEventsManager(mMatchingKey);
    }

    @NonNull
    private ClientComponentsRegisterImpl getRegister(boolean largeSegmentsEnabled) {
        return new ClientComponentsRegisterImpl(
                new SplitClientConfig.Builder()
                        .largeSegmentsEnabled(largeSegmentsEnabled)
                        .build(),
                mMySegmentsSynchronizerFactory,
                mStorageContainer,
                mAttributesSynchronizerFactory,
                mAttributesSynchronizerRegistry,
                mMySegmentsSynchronizerRegistry,
                mMySegmentsUpdateWorkerRegistry,
                mEventsManagerRegistry,
                mSseAuthenticator,
                mMySegmentsNotificationProcessorRegistry,
                mMySegmentsNotificationProcessorFactory,
                mMySegmentsV2PayloadDecoder
        );
    }
}
