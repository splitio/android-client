package io.split.android.client.service.telemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.work.WorkManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.service.synchronizer.SynchronizerImpl;
import io.split.android.client.service.synchronizer.WorkManagerWrapper;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class SynchronizerImplTelemetryTest {

    @Mock
    SplitTaskExecutor mTaskExecutor;
    @Mock
    SplitApiFacade mSplitApiFacade;
    @Mock
    SplitStorageContainer mSplitStorageContainer;
    @Mock
    PersistentSplitsStorage mPersistentSplitsStorageContainer;
    @Mock
    PersistentEventsStorage mEventsStorage;
    @Mock
    PersistentImpressionsStorage mImpressionsStorage;
    @Mock
    SplitTaskExecutionListener mTaskExecutionListener;
    @Mock
    TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    @Mock
    RetryBackoffCounterTimerFactory mRetryBackoffFactory;
    @Mock
    RetryBackoffCounterTimer mRetryTimerSplitsSync;
    @Mock
    RetryBackoffCounterTimer mRetryTimerSplitsUpdate;
    @Mock
    RetryBackoffCounterTimer mRetryTimerMySegmentsSync;
    @Mock
    WorkManager mWorkManager;
    @Mock
    SplitTaskFactory mTaskFactory;
    @Mock
    SplitEventsManager mEventsManager;
    @Mock
    WorkManagerWrapper mWorkManagerWrapper;
    @Mock
    SplitClientConfig mConfig;

    private SynchronizerImpl mSynchronizer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void eventsSyncIsTrackedInTelemetry() {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        EventsRecorderTask eventsRecorderTask = mock(EventsRecorderTask.class);
        when(eventsRecorderTask.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.EVENTS_RECORDER));

        SplitStorageContainer mSplitStorageContainer = mock(SplitStorageContainer.class);
        when(mSplitStorageContainer.getEventsStorage()).thenReturn(mock(PersistentEventsStorage.class));
        when(mSplitStorageContainer.getImpressionsStorage()).thenReturn(mock(PersistentImpressionsStorage.class));

        SplitTaskFactory mTaskFactory = mock(SplitTaskFactory.class);
        when(mTaskFactory.createEventsRecorderTask()).thenReturn(eventsRecorderTask);
        when(mTaskFactory.createSplitsSyncTask(anyBoolean())).thenReturn(mock(SplitsSyncTask.class));

        SplitTaskExecutor mTaskExecutor = mock(SplitTaskExecutor.class);
        when(mTaskExecutor.schedule(eq(eventsRecorderTask), anyLong(), anyLong(), any())).thenReturn("id");

        RetryBackoffCounterTimerFactory mRetryBackoffCounterFactory = mock(RetryBackoffCounterTimerFactory.class);
        when(mRetryBackoffCounterFactory.create(mTaskExecutor, 1)).thenReturn(mock(RetryBackoffCounterTimer.class));

        SynchronizerImpl sync = new SynchronizerImpl(
                mConfig,
                mTaskExecutor,
                mSplitStorageContainer,
                mTaskFactory,
                mEventsManager,
                mWorkManagerWrapper,
                mRetryBackoffCounterFactory,
                mTelemetryRuntimeProducer
        );

        verify(mTelemetryRuntimeProducer).recordSuccessfulSync(eq(OperationType.EVENTS), anyLong());
    }
}
