package io.split.android.client.service.impressions.strategy

import io.split.android.client.dtos.KeyImpression
import io.split.android.client.service.executor.SplitTaskExecutionInfo
import io.split.android.client.service.executor.SplitTaskExecutionInfo.DO_NOT_RETRY
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.executor.SplitTaskSerialWrapper
import io.split.android.client.service.executor.SplitTaskType
import io.split.android.client.service.impressions.*
import io.split.android.client.service.impressions.observer.ImpressionsObserver
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer
import io.split.android.client.service.synchronizer.RecorderSyncHelper
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class OptimizedTrackerTest {

    @Mock
    private lateinit var taskExecutor: SplitTaskExecutor

    @Mock
    private lateinit var taskFactory: ImpressionsTaskFactory

    @Mock
    private lateinit var impressionTimer: RetryBackoffCounterTimer

    @Mock
    private lateinit var syncHelper: RecorderSyncHelper<KeyImpression>

    @Mock
    private lateinit var impressionsObserver: ImpressionsObserver

    private lateinit var tracker: OptimizedTracker

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        tracker = OptimizedTracker(
            impressionsObserver,
            syncHelper,
            taskExecutor,
            taskFactory,
            impressionTimer,
            30,
            true
        )
    }

    @Test
    fun `start periodic recording schedules impression recorder task`() {
        val task = mock(ImpressionsRecorderTask::class.java)
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(task)

        tracker.startPeriodicRecording()

        verify(taskExecutor).schedule(task, 0L, 30L, syncHelper)
    }

    @Test
    fun `flush flushes impressions`() {
        val task = mock(ImpressionsRecorderTask::class.java)
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(task)

        tracker.flush()

        verify(impressionTimer).setTask(task, syncHelper)
        verify(impressionTimer).start()
    }

    @Test
    fun `start periodic recording schedules impression recording`() {
        val task = mock(ImpressionsRecorderTask::class.java)
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(task)

        tracker.startPeriodicRecording()

        verify(taskExecutor).schedule(task, 0L, 30L, syncHelper)
    }

    @Test
    fun `stop periodic recording stops impression recording`() {
        val task = mock(ImpressionsRecorderTask::class.java)
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(task)
        `when`(taskExecutor.schedule(task, 0L, 30L, syncHelper)).thenReturn("250")

        tracker.run {
            startPeriodicRecording()
            stopPeriodicRecording()
        }

        verify(taskExecutor).stopTask("250")
    }

    @Test
    fun `stopPeriodicRecording calls persist on ImpressionsObserver`() {
        tracker.stopPeriodicRecording()

        verify(impressionsObserver).persist()
    }

    @Test
    fun `call stop periodic tracking when sync listener returns do not retry`() {
        val listenerCaptor = ArgumentCaptor.forClass(SplitTaskExecutionListener::class.java)

        val impressionsRecorderTask = mock(ImpressionsRecorderTask::class.java)
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(impressionsRecorderTask)
        `when`(taskExecutor.schedule(eq(impressionsRecorderTask), eq(0L), eq(30L), any())).thenReturn("250")
        `when`(syncHelper.addListener(listenerCaptor.capture())).thenAnswer { it }
        `when`(syncHelper.taskExecuted(argThat {
            it.taskType == SplitTaskType.IMPRESSIONS_RECORDER
        })).thenAnswer {
            listenerCaptor.value.taskExecuted(
                SplitTaskExecutionInfo.error(
                    SplitTaskType.IMPRESSIONS_RECORDER,
                    mapOf(DO_NOT_RETRY to true)
                )
            )
            it
        }

        tracker = OptimizedTracker(
            impressionsObserver,
            syncHelper,
            taskExecutor,
            taskFactory,
            impressionTimer,
            30,
            true
        )

        val spy = spy(tracker)
        tracker.startPeriodicRecording()
        // simulate sync helper trigger
        syncHelper.taskExecuted(
            SplitTaskExecutionInfo.error(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mapOf(DO_NOT_RETRY to true)
            )
        )

        // start periodic recording again to verify it is not working anymore
        spy.startPeriodicRecording()

        verify(taskExecutor).stopTask("250")
        verify(impressionsObserver).persist()
    }
}
