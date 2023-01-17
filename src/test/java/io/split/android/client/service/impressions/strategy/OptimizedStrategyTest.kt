package io.split.android.client.service.impressions.strategy

import io.split.android.client.dtos.KeyImpression
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.impressions.ImpressionsCounter
import io.split.android.client.service.impressions.ImpressionsObserver
import io.split.android.client.service.impressions.ImpressionsRecorderTask
import io.split.android.client.service.impressions.ImpressionsTaskFactory
import io.split.android.client.service.synchronizer.RecorderSyncHelper
import io.split.android.client.telemetry.model.ImpressionsDataType
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class OptimizedStrategyTest {

    @Mock
    private lateinit var taskExecutor: SplitTaskExecutor

    @Mock
    private lateinit var taskFactory: ImpressionsTaskFactory

    @Mock
    private lateinit var impressionsCounter: ImpressionsCounter

    @Mock
    private lateinit var impressionsObserver: ImpressionsObserver

    @Mock
    private lateinit var impressionsSyncHelper: RecorderSyncHelper<KeyImpression>

    @Mock
    private lateinit var telemetryRuntimeProducer: TelemetryRuntimeProducer

    private lateinit var strategy: OptimizedStrategy

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        strategy = OptimizedStrategy(
            impressionsObserver,
            impressionsCounter,
            impressionsSyncHelper,
            taskExecutor,
            taskFactory,
            telemetryRuntimeProducer
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

        verify(telemetryRuntimeProducer).recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 1)
        verify(telemetryRuntimeProducer, never()).recordImpressionStats(ImpressionsDataType.IMPRESSIONS_DEDUPED, 1)
    }

    @Test
    fun `deduped impressions are tracked in telemetry`() {
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(mock(ImpressionsRecorderTask::class.java))
        `when`(impressionsSyncHelper.pushAndCheckIfFlushNeeded(any())).thenReturn(false)

        strategy.apply(createUniqueImpression(time = 10))
        strategy.apply(createUniqueImpression(time = 2000000000L))

        verify(telemetryRuntimeProducer).recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 1)
        verify(telemetryRuntimeProducer).recordImpressionStats(ImpressionsDataType.IMPRESSIONS_DEDUPED, 1)
    }
}
