package io.split.android.client.service.telemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.executor.SplitSingleThreadTaskExecutor;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.impressions.ImpressionManager;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.SynchronizerImpl;
import io.split.android.client.service.synchronizer.WorkManagerWrapper;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegistryImpl;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistryImpl;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class SynchronizerImplTelemetryTest {

    @Mock
    TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    @Mock
    SplitEventsManager mEventsManager;
    @Mock
    WorkManagerWrapper mWorkManagerWrapper;
    @Mock
    SplitClientConfig mConfig;
    @Mock
    AttributesSynchronizerRegistryImpl mAttributesSynchronizerRegistry;
    @Mock
    MySegmentsSynchronizerRegistryImpl mMySegmentsSynchronizerRegistry;
    @Mock
    ImpressionManager mImpressionManager;

    private SynchronizerImpl mSynchronizer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        EventsRecorderTask eventsRecorderTask = mock(EventsRecorderTask.class);
        when(eventsRecorderTask.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.EVENTS_RECORDER));

        SplitStorageContainer mSplitStorageContainer = mock(SplitStorageContainer.class);
        when(mSplitStorageContainer.getEventsStorage()).thenReturn(mock(PersistentEventsStorage.class));
        when(mSplitStorageContainer.getImpressionsStorage()).thenReturn(mock(PersistentImpressionsStorage.class));

        SplitTaskFactory mTaskFactory = mock(SplitTaskFactory.class);
        when(mTaskFactory.createEventsRecorderTask()).thenReturn(eventsRecorderTask);
        when(mTaskFactory.createSplitsSyncTask(anyBoolean())).thenReturn(mock(SplitsSyncTask.class));

        SplitTaskExecutor mTaskExecutor = mock(SplitTaskExecutor.class);
        SplitTaskExecutor mSingleThreadTaskExecutor = mock(SplitTaskExecutor.class);
        when(mTaskExecutor.schedule(eq(eventsRecorderTask), anyLong(), anyLong(), any())).thenReturn("id");
        when(mTaskExecutor.schedule(argThat(argument -> argument instanceof SplitsSyncTask), anyLong(), anyLong(), any())).thenReturn("id");

        RetryBackoffCounterTimerFactory mRetryBackoffCounterFactory = mock(RetryBackoffCounterTimerFactory.class);
        when(mRetryBackoffCounterFactory.create(mTaskExecutor, 1)).thenReturn(mock(RetryBackoffCounterTimer.class));
        when(mRetryBackoffCounterFactory.create(mSingleThreadTaskExecutor, 1)).thenReturn(mock(RetryBackoffCounterTimer.class));

        mSynchronizer = new SynchronizerImpl(
                mConfig,
                mTaskExecutor,
                mSingleThreadTaskExecutor,
                mSplitStorageContainer,
                mTaskFactory,
                mEventsManager,
                mWorkManagerWrapper,
                mRetryBackoffCounterFactory,
                mTelemetryRuntimeProducer,
                mAttributesSynchronizerRegistry,
                mMySegmentsSynchronizerRegistry,
                mImpressionManager);
    }

    @Test
    public void pushEventRecordsInTelemetry() {

        mSynchronizer.pushEvent(new Event());

        verify(mTelemetryRuntimeProducer).recordEventStats(EventsDataRecordsEnum.EVENTS_QUEUED, 1);
    }
}
