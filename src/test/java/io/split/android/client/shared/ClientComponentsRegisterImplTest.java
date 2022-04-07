package io.split.android.client.shared;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.api.Key;
import io.split.android.client.events.EventsManagerRegistry;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessorFactory;
import io.split.android.client.service.sseclient.notifications.mysegments.MySegmentsNotificationProcessorRegistry;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorkerRegistry;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticator;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerFactory;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegistry;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizer;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerFactory;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistry;
import io.split.android.client.storage.SplitStorageContainer;

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
    private SplitEventsManager mSplitEventsManager;
    @Mock
    private MySegmentsSynchronizer mMySegmentsSynchronizer;

    private final Key mMatchingKey = new Key("matching_key");

    private ClientComponentsRegisterImpl register;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        String mDefaultMatchingKey = "matching_key";

        when(mMySegmentsSynchronizerFactory.getSynchronizer(mMySegmentsTaskFactory, mSplitEventsManager))
                .thenReturn(mMySegmentsSynchronizer);

        register = new ClientComponentsRegisterImpl(
                mMySegmentsSynchronizerFactory,
                mStorageContainer,
                mAttributesSynchronizerFactory,
                mAttributesSynchronizerRegistry,
                mMySegmentsSynchronizerRegistry,
                mMySegmentsUpdateWorkerRegistry,
                mEventsManagerRegistry,
                mSseAuthenticator,
                mMySegmentsNotificationProcessorRegistry,
                mDefaultMatchingKey,
                mMySegmentsNotificationProcessorFactory,
                mMySegmentsV2PayloadDecoder
        );
    }

    @Test
    public void attributesSynchronizerIsRegistered() {
        register.registerComponents(mMatchingKey, mMySegmentsTaskFactory, mSplitEventsManager);

        verify(mAttributesSynchronizerRegistry).registerAttributesSynchronizer(eq("matching_key"), any());
    }

    @Test
    public void mySegmentsSynchronizerIsRegistered() {
        register.registerComponents(mMatchingKey, mMySegmentsTaskFactory, mSplitEventsManager);

        verify(mMySegmentsSynchronizerRegistry).registerMySegmentsSynchronizer("matching_key", mMySegmentsSynchronizer);
    }

    @Test
    public void mySegmentsUpdateWorkerIsRegistered() {
        register.registerComponents(mMatchingKey, mMySegmentsTaskFactory, mSplitEventsManager);

        verify(mMySegmentsUpdateWorkerRegistry).registerMySegmentsUpdateWorker(eq("matching_key"), any());
    }

    @Test
    public void mySegmentsNotificationProcessorIsRegistered() {
        register.registerComponents(mMatchingKey, mMySegmentsTaskFactory, mSplitEventsManager);

        verify(mMySegmentsNotificationProcessorRegistry).registerMySegmentsProcessor(eq("matching_key"), any());
    }

    @Test
    public void eventsManagerIsRegistered() {
        register.registerComponents(mMatchingKey, mMySegmentsTaskFactory, mSplitEventsManager);

        verify(mEventsManagerRegistry).registerEventsManager("matching_key", mSplitEventsManager);
    }
}
