package io.split.android.client.service.impressions.strategy

import io.split.android.client.dtos.KeyImpression
import io.split.android.client.service.ServiceConstants
import io.split.android.client.service.executor.SplitTaskExecutionInfo
import io.split.android.client.service.executor.SplitTaskExecutionInfo.DO_NOT_RETRY
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.executor.SplitTaskType
import io.split.android.client.service.impressions.ImpressionsCounter
import io.split.android.client.service.impressions.ImpressionsRecorderTask
import io.split.android.client.service.impressions.ImpressionsTaskFactory
import io.split.android.client.service.impressions.observer.ImpressionsObserverImpl
import io.split.android.client.service.synchronizer.RecorderSyncHelper
import io.split.android.client.telemetry.model.ImpressionsDataType
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.argThat
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock

class OptimizedStrategyTest {

    @Mock
    private lateinit var taskExecutor: SplitTaskExecutor

    @Mock
    private lateinit var taskFactory: ImpressionsTaskFactory

    @Mock
    private lateinit var impressionsCounter: ImpressionsCounter

    @Mock
    private lateinit var impressionsObserver: ImpressionsObserverImpl

    @Mock
    private lateinit var impressionsSyncHelper: RecorderSyncHelper<KeyImpression>

    @Mock
    private lateinit var telemetryRuntimeProducer: TelemetryRuntimeProducer

    private lateinit var strategy: OptimizedStrategy

    private val dedupeTimeInterval = ServiceConstants.DEFAULT_IMPRESSIONS_DEDUPE_TIME_INTERVAL

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        strategy = OptimizedStrategy(
            impressionsObserver,
            impressionsCounter,
            impressionsSyncHelper,
            taskExecutor,
            taskFactory,
            telemetryRuntimeProducer,
            dedupeTimeInterval
        )
    }

    @Test
    fun `count is not incremented when previous time does not exist`() {
        strategy.apply(createUniqueImpression())

        verifyNoInteractions(taskExecutor)
    }

    @Test
    fun `count is incremented when previous time exists`() {
        `when`(impressionsObserver.testAndSet(any()))
            .thenReturn(null)
            .thenReturn(200L)
            .thenReturn(300L)

        strategy.apply(createUniqueImpression("splitName"))
        strategy.apply(createUniqueImpression("splitName"))
        strategy.apply(createUniqueImpression("splitName"))

        verify(impressionsCounter, times(2)).inc(eq("splitName"), anyLong(), anyInt())
    }

    @Test
    fun `impression is flushed when flush is needed`() {
        val impressionsTask = mock(ImpressionsRecorderTask::class.java)
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(impressionsTask)
        `when`(impressionsSyncHelper.pushAndCheckIfFlushNeeded(any())).thenReturn(true)

        strategy.apply(createUniqueImpression(time = 1000000000L))

        verify(taskExecutor).submit(
            any(ImpressionsRecorderTask::class.java),
            eq<SplitTaskExecutionListener?>(impressionsSyncHelper)
        )
    }

    @Test
    fun `impression is not flushed when flush is not needed`() {
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(mock(ImpressionsRecorderTask::class.java))
        `when`(impressionsSyncHelper.pushAndCheckIfFlushNeeded(any())).thenReturn(false)

        strategy.apply(createUniqueImpression(time = 1000000000L))

        verifyNoInteractions(taskExecutor)
    }

    @Test
    fun `recorded impressions are tracked in telemetry`() {
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(mock(ImpressionsRecorderTask::class.java))
        `when`(impressionsSyncHelper.pushAndCheckIfFlushNeeded(any())).thenReturn(true)

        strategy.apply(createUniqueImpression(time = 1000000000L))

        verify(telemetryRuntimeProducer).recordImpressionStats(
            ImpressionsDataType.IMPRESSIONS_QUEUED,
            1
        )
        verify(
            telemetryRuntimeProducer,
            never()
        ).recordImpressionStats(ImpressionsDataType.IMPRESSIONS_DEDUPED, 1)
    }

    @Test
    fun `deduped impressions are tracked in telemetry`() {
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(mock(ImpressionsRecorderTask::class.java))
        `when`(impressionsSyncHelper.pushAndCheckIfFlushNeeded(any())).thenReturn(false)

        strategy.apply(createUniqueImpression(time = 10))
        strategy.apply(createUniqueImpression(time = 2000000000L))

        verify(telemetryRuntimeProducer).recordImpressionStats(
            ImpressionsDataType.IMPRESSIONS_QUEUED,
            1
        )
        verify(telemetryRuntimeProducer).recordImpressionStats(
            ImpressionsDataType.IMPRESSIONS_DEDUPED,
            1
        )
    }

    @Test
    fun `do not submit recording task when push fails with do not retry`() {
        val listenerCaptor = ArgumentCaptor.forClass(SplitTaskExecutionListener::class.java)

        val listenerInvocation: (invocation: InvocationOnMock) -> Any = { invocationOnMock ->
            listenerCaptor.value.taskExecuted(
                SplitTaskExecutionInfo.error(
                    SplitTaskType.IMPRESSIONS_RECORDER,
                    mapOf(DO_NOT_RETRY to true)
                )
            )
            invocationOnMock
        }
        val impressionsRecorderTask = mock(ImpressionsRecorderTask::class.java)
        `when`(impressionsRecorderTask.execute()).thenReturn(
            SplitTaskExecutionInfo.error(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mapOf(DO_NOT_RETRY to true)
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

        strategy = OptimizedStrategy(
            impressionsObserver,
            impressionsCounter,
            impressionsSyncHelper,
            taskExecutor,
            taskFactory,
            telemetryRuntimeProducer,
            dedupeTimeInterval
        )

        // call apply two times; first one will trigger the recording task and second one should not
        strategy.apply(createUniqueImpression(time = 1000000000L))
        strategy.apply(createUniqueImpression(time = 2000000000L))

        verify(taskExecutor, times(1)).submit(
            any(ImpressionsRecorderTask::class.java),
            eq<SplitTaskExecutionListener?>(impressionsSyncHelper)
        )
    }

    @Test
    fun `impressions observer is not called when impression has properties`() {
        val impression = createUniqueImpression(time = 1000000000L, propertiesJson = "{\"key\":\"value\"}")
        val impression2 = createUniqueImpression(time = 1000000000L, propertiesJson = "{\"key\":\"value\"}")

        strategy.apply(impression)
        strategy.apply(impression2)

        verify(impressionsObserver, times(0)).testAndSet(impression)
    }
}
