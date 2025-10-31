package io.split.android.client.service.impressions.strategy

import io.split.android.client.dtos.KeyImpression
import io.split.android.client.service.executor.SplitTaskExecutionInfo
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.executor.SplitTaskType
import io.split.android.client.service.impressions.ImpressionsRecorderTask
import io.split.android.client.service.impressions.ImpressionsTaskFactory
import io.split.android.client.service.impressions.observer.ImpressionsObserver
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer
import io.split.android.client.service.synchronizer.RecorderSyncHelper
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class DebugTrackerTest {

    @Mock
    private lateinit var impressionsObserver: ImpressionsObserver
    @Mock
    private lateinit var syncHelper: RecorderSyncHelper<KeyImpression>
    @Mock
    private lateinit var taskExecutor: SplitTaskExecutor
    @Mock
    private lateinit var taskFactory: ImpressionsTaskFactory
    @Mock
    private lateinit var retryBackoffCounterTimer: RetryBackoffCounterTimer

    private lateinit var tracker: DebugTracker

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        tracker = DebugTracker(
            impressionsObserver,
            syncHelper,
            taskExecutor,
            taskFactory,
            retryBackoffCounterTimer,
            30
        )
    }

    @Test
    fun `flush flushes impressions`() {
        val task = mock(ImpressionsRecorderTask::class.java)
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(task)

        tracker.flush()

        verify(retryBackoffCounterTimer).setTask(task, syncHelper)
        verify(retryBackoffCounterTimer).start()
    }

    @Test
    fun `start periodic recording`() {
        val task = mock(ImpressionsRecorderTask::class.java)
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(task)

        tracker.startPeriodicRecording()

        verify(taskExecutor).schedule(task, 0L, 30L, syncHelper)
    }

    @Test
    fun `stop periodic recording`() {
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
    fun `periodic recording is task is cancelled before being rescheduled`() {
        val task = mock(ImpressionsRecorderTask::class.java)
        val task2 = mock(ImpressionsRecorderTask::class.java)
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(task).thenReturn(task2)
        `when`(taskExecutor.schedule(task, 0L, 30L, syncHelper)).thenReturn("250")
        `when`(taskExecutor.schedule(task2, 0L, 30L, syncHelper)).thenReturn("251")

        tracker.run {
            startPeriodicRecording()
            startPeriodicRecording()
        }

        verify(taskExecutor).schedule(task, 0L, 30L, syncHelper)
        verify(taskExecutor).stopTask("250")
        verify(taskExecutor).schedule(task2, 0L, 30L, syncHelper)
    }

    @Test
    fun `stopPeriodicRecording calls persist on observer`() {
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
                    mapOf(SplitTaskExecutionInfo.DO_NOT_RETRY to true)
                )
            )
            it
        }

        tracker = DebugTracker(
            impressionsObserver,
            syncHelper,
            taskExecutor,
            taskFactory,
            retryBackoffCounterTimer,
            30
        )

        tracker.startPeriodicRecording()
        val spy = spy(tracker)
        // simulate sync helper trigger
        syncHelper.taskExecuted(
            SplitTaskExecutionInfo.error(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mapOf(SplitTaskExecutionInfo.DO_NOT_RETRY to true)
            )
        )

        // start periodic recording again to verify it is not working anymore
        spy.startPeriodicRecording()

        verify(taskExecutor).stopTask("250")
        verify(impressionsObserver).persist()
    }
}
