package io.split.android.client.service.impressions.strategy

import io.split.android.client.dtos.KeyImpression
import io.split.android.client.impressions.Impression
import io.split.android.client.service.executor.SplitTaskExecutionInfo
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.executor.SplitTaskType
import io.split.android.client.service.impressions.ImpressionsRecorderTask
import io.split.android.client.service.impressions.ImpressionsTaskFactory
import io.split.android.client.service.impressions.observer.ImpressionsObserver
import io.split.android.client.service.synchronizer.RecorderSyncHelper
import io.split.android.client.telemetry.model.ImpressionsDataType
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock

class DebugStrategyTest {

    @Mock
    private lateinit var taskExecutor: SplitTaskExecutor

    @Mock
    private lateinit var taskFactory: ImpressionsTaskFactory

    @Mock
    private lateinit var impressionsSyncHelper: RecorderSyncHelper<KeyImpression>

    @Mock
    private lateinit var telemetryRuntimeProducer: TelemetryRuntimeProducer

    @Mock
    private lateinit var impressionsObserver: ImpressionsObserver

    private lateinit var strategy: DebugStrategy

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        strategy = DebugStrategy(
            impressionsObserver,
            impressionsSyncHelper,
            taskExecutor,
            taskFactory,
            telemetryRuntimeProducer,
        )
    }

    @Test
    fun `recorded impression is tracked in telemetry`() {
        strategy.apply(createUniqueImpression())

        verify(telemetryRuntimeProducer).recordImpressionStats(
            ImpressionsDataType.IMPRESSIONS_QUEUED,
            1
        )
    }

    @Test
    fun `impression are flushed when flush is needed`() {
        val impressionsRecorderTask = mock(ImpressionsRecorderTask::class.java)
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(impressionsRecorderTask)
        `when`(impressionsSyncHelper.pushAndCheckIfFlushNeeded(any())).thenReturn(true)

        strategy.apply(createUniqueImpression())

        verify(taskExecutor).submit(impressionsRecorderTask, impressionsSyncHelper)
    }

    @Test
    fun `impression are not flushed when flush is not needed`() {
        val impressionsRecorderTask = mock(ImpressionsRecorderTask::class.java)
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(impressionsRecorderTask)
        `when`(impressionsSyncHelper.pushAndCheckIfFlushNeeded(any())).thenReturn(false)

        strategy.apply(createUniqueImpression())

        verify(taskExecutor, never()).submit(impressionsRecorderTask, impressionsSyncHelper)
    }

    @Test
    fun `apply calls testAndSet on observer`() {
        val impression = createUniqueImpression()
        strategy.apply(impression)

        verify(impressionsObserver).testAndSet(impression)
    }

    @Test
    fun `withPreviousTime is called on impression`() {
        val impression = createUniqueImpression()
        `when`(impressionsObserver.testAndSet(impression)).thenReturn(20421)
        strategy.apply(impression)

        spy(impression).withPreviousTime(20421)
    }

//    @Test
//    fun `call stop periodic tracking when sync listener returns do not retry`() {
//        val listenerCaptor = ArgumentCaptor.forClass(SplitTaskExecutionListener::class.java)
//
//        `when`(impressionsSyncHelper.addListener(listenerCaptor.capture())).thenAnswer { it }
//        `when`(impressionsSyncHelper.taskExecuted(argThat {
//            it.taskType == SplitTaskType.IMPRESSIONS_RECORDER
//        })).thenAnswer {
//            listenerCaptor.value.taskExecuted(
//                SplitTaskExecutionInfo.error(
//                    SplitTaskType.IMPRESSIONS_RECORDER,
//                    mapOf(SplitTaskExecutionInfo.DO_NOT_RETRY to true)
//                )
//            )
//            it
//        }
//
//        strategy = DebugStrategy(
//            impressionsObserver,
//            impressionsSyncHelper,
//            taskExecutor,
//            taskFactory,
//            telemetryRuntimeProducer,
//        )
//
//        strategy.startPeriodicRecording()
//        // simulate sync helper trigger
//        impressionsSyncHelper.taskExecuted(
//            SplitTaskExecutionInfo.error(
//                SplitTaskType.IMPRESSIONS_RECORDER,
//                mapOf(SplitTaskExecutionInfo.DO_NOT_RETRY to true)
//            )
//        )
//
//        // start periodic recording again to verify it is not working anymore
//        strategy.startPeriodicRecording()
//
//        verify(tracker, times(1)).startPeriodicRecording()
//        verify(tracker).stopPeriodicRecording()
//    }

    @Test
    fun `do not submit recording task when push fails with do not retry`() {
        val listenerCaptor = ArgumentCaptor.forClass(SplitTaskExecutionListener::class.java)

        val listenerInvocation: (invocation: InvocationOnMock) -> Any = { invocationOnMock ->
            listenerCaptor.value.taskExecuted(
                SplitTaskExecutionInfo.error(
                    SplitTaskType.IMPRESSIONS_RECORDER,
                    mapOf(SplitTaskExecutionInfo.DO_NOT_RETRY to true)
                )
            )
            invocationOnMock
        }
        val impressionsRecorderTask = mock(ImpressionsRecorderTask::class.java)
        `when`(impressionsRecorderTask.execute()).thenReturn(
            SplitTaskExecutionInfo.error(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mapOf(SplitTaskExecutionInfo.DO_NOT_RETRY to true)
            )
        )
        `when`(
            taskExecutor.submit(
                any(ImpressionsRecorderTask::class.java),
                eq<SplitTaskExecutionListener?>(impressionsSyncHelper)
            )
        ).thenAnswer(listenerInvocation)
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(impressionsRecorderTask)
        `when`(impressionsSyncHelper.addListener(listenerCaptor.capture())).thenAnswer { it }
        `when`(impressionsSyncHelper.taskExecuted(argThat {
            it.taskType == SplitTaskType.IMPRESSIONS_RECORDER
        })).thenAnswer(listenerInvocation)
        `when`(impressionsSyncHelper.pushAndCheckIfFlushNeeded(any())).thenReturn(true)

        strategy = DebugStrategy(
            impressionsObserver,
            impressionsSyncHelper,
            taskExecutor,
            taskFactory,
            telemetryRuntimeProducer,
        )

        // call apply two times; first one will trigger the recording task and second one should not
        strategy.apply(createUniqueImpression(time = 1000000000L))
        strategy.apply(createUniqueImpression(time = 2000000000L))

        verify(taskExecutor, times(1)).submit(
            any(ImpressionsRecorderTask::class.java),
            eq<SplitTaskExecutionListener?>(impressionsSyncHelper)
        )
    }
}
