package io.split.android.client.service.impressions.strategy

import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.executor.SplitTaskSerialWrapper
import io.split.android.client.service.impressions.*
import io.split.android.client.service.impressions.unique.SaveUniqueImpressionsTask
import io.split.android.client.service.impressions.unique.UniqueKeysRecorderTask
import io.split.android.client.service.impressions.unique.UniqueKeysTracker
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.mockito.Mockito.*

class NoneTrackerTest {

    @Mock
    private lateinit var taskExecutor: SplitTaskExecutor
    @Mock
    private lateinit var taskFactory: ImpressionsTaskFactory
    @Mock
    private lateinit var impressionTimer: RetryBackoffCounterTimer
    @Mock
    private lateinit var uniqueKeysTimer: RetryBackoffCounterTimer
    @Mock
    private lateinit var impressionCounter: ImpressionsCounter
    @Mock
    private lateinit var uniqueKeysTracker: UniqueKeysTracker
    private lateinit var tracker: NoneTracker

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        tracker = NoneTracker(
            taskExecutor,
            taskFactory,
            impressionCounter,
            uniqueKeysTracker,
            impressionTimer,
            uniqueKeysTimer,
            30,
            40,
            true
        )
    }

    @Test
    fun `flush flushes impression count`() {
        val saveTask = mock(SaveImpressionsCountTask::class.java)
        val recorderTask = mock(ImpressionsCountRecorderTask::class.java)
        `when`(taskFactory.createSaveImpressionsCountTask(any())).thenReturn(saveTask)
        `when`(taskFactory.createImpressionsCountRecorderTask()).thenReturn(recorderTask)

        tracker.flush()

        verify(impressionTimer).setTask(argThat<SplitTaskSerialWrapper> { argument ->
            val taskList = argument.taskList
            taskList.size == 2 && taskList[0] == saveTask && taskList[1] == recorderTask
        })
        verify(impressionTimer).start()
    }

    @Test
    fun `flush flushes unique keys`() {
        val saveTask = mock(SaveUniqueImpressionsTask::class.java)
        val recorderTask = mock(UniqueKeysRecorderTask::class.java)
        `when`(taskFactory.createSaveUniqueImpressionsTask(any())).thenReturn(saveTask)
        `when`(taskFactory.createUniqueImpressionsRecorderTask()).thenReturn(recorderTask)

        tracker.flush()

        verify(uniqueKeysTimer).setTask(argThat<SplitTaskSerialWrapper> { argument ->
            val taskList = argument.taskList
            taskList.size == 2 && taskList[0] == saveTask && taskList[1] == recorderTask
        })
        verify(uniqueKeysTimer).start()
    }

    @Test
    fun `start periodic recording schedules impression count recorder task`() {
        val task = mock(ImpressionsCountRecorderTask::class.java)
        `when`(taskFactory.createImpressionsCountRecorderTask()).thenReturn(task)

        tracker.startPeriodicRecording()

        verify(taskExecutor).schedule(task, 0L, 30L, null)
    }

    @Test
    fun `start periodic recording schedules unique keys recorder task`() {
        val task = mock(UniqueKeysRecorderTask::class.java)
        `when`(taskFactory.createUniqueImpressionsRecorderTask()).thenReturn(task)

        tracker.startPeriodicRecording()

        verify(taskExecutor).schedule(task, 0L, 40L, null)
    }

    @Test
    fun `stop periodic recording stops running tasks`() {
        val countTask = mock(ImpressionsCountRecorderTask::class.java)
        `when`(taskFactory.createImpressionsCountRecorderTask()).thenReturn(countTask)
        val uniqueKeysTask = mock(UniqueKeysRecorderTask::class.java)
        `when`(taskFactory.createUniqueImpressionsRecorderTask()).thenReturn(uniqueKeysTask)
        `when`(
            taskExecutor.schedule(
                any(ImpressionsCountRecorderTask::class.java),
                eq(0L),
                eq(30L),
                eq<SplitTaskExecutionListener?>(null)
            )
        ).thenReturn("id_1")
        `when`(
            taskExecutor.schedule(
                any(UniqueKeysRecorderTask::class.java),
                eq(0L),
                eq(40L),
                eq<SplitTaskExecutionListener?>(null)
            )
        ).thenReturn("id_2")

        tracker.startPeriodicRecording()
        tracker.stopPeriodicRecording()

        verify(taskExecutor).stopTask("id_1")
        verify(taskExecutor).stopTask("id_2")
    }

    @Test
    fun `stop periodic recording saves impression count`() {
        `when`(taskFactory.createSaveImpressionsCountTask(any())).thenReturn(mock(SaveImpressionsCountTask::class.java))

        tracker.stopPeriodicRecording()

        verify(taskExecutor).submit(
            any(SaveImpressionsCountTask::class.java),
            eq<SplitTaskExecutionListener?>(null)
        )
    }

    @Test
    fun `stop periodic recording saves unique keys`() {
        `when`(taskFactory.createSaveUniqueImpressionsTask(any())).thenReturn(mock(SaveUniqueImpressionsTask::class.java))

        tracker.stopPeriodicRecording()

        verify(taskExecutor).submit(
            any(SaveUniqueImpressionsTask::class.java),
            eq<SplitTaskExecutionListener?>(null)
        )
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
    fun `stop periodic recording does not save unique keys when tracking is disabled`() {
        val countTask = mock(SaveUniqueImpressionsTask::class.java)
        `when`(taskFactory.createSaveUniqueImpressionsTask(any())).thenReturn(countTask)
        `when`(
            taskExecutor.schedule(
                any(SaveUniqueImpressionsTask::class.java),
                eq(0L),
                eq(40L),
                eq<SplitTaskExecutionListener?>(null)
            )
        ).thenReturn("id_1")

        tracker.enableTracking(false)
        tracker.stopPeriodicRecording()

        verify(taskExecutor, never()).submit(eq(countTask), any())
    }
}
