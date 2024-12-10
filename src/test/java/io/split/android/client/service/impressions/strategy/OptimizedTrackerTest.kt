package io.split.android.client.service.impressions.strategy

import io.split.android.client.dtos.KeyImpression
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.executor.SplitTaskSerialWrapper
import io.split.android.client.service.impressions.*
import io.split.android.client.service.impressions.observer.ImpressionsObserver
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer
import io.split.android.client.service.synchronizer.RecorderSyncHelper
import org.junit.Before
import org.junit.Test
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
    fun `stop periodic recording does not save impression count when tracking is disabled`() {
        val countTask = mock(SaveImpressionsCountTask::class.java)
        `when`(taskFactory.createSaveImpressionsCountTask(any())).thenReturn(countTask)
        `when`(
            taskExecutor.schedule(
                any(SaveImpressionsCountTask::class.java),
                eq(0L),
                eq(40L),
                eq<SplitTaskExecutionListener?>(null)
            )
        ).thenReturn("id_1")
        tracker.enableTracking(false)
        tracker.stopPeriodicRecording()

        verify(taskExecutor, never()).submit(eq(countTask), any())
    }

    @Test
    fun `stopPeriodicRecording calls persist on ImpressionsObserver`() {
        tracker.stopPeriodicRecording()

        verify(impressionsObserver).persist()
    }
}
