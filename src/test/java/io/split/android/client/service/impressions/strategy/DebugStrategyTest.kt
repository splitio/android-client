package io.split.android.client.service.impressions.strategy

import io.split.android.client.dtos.KeyImpression
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.impressions.ImpressionsObserver
import io.split.android.client.service.impressions.ImpressionsRecorderTask
import io.split.android.client.service.impressions.ImpressionsTaskFactory
import io.split.android.client.service.sseclient.sseclient.BackoffCounterTimer
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer
import io.split.android.client.service.synchronizer.RecorderSyncHelper
import io.split.android.client.telemetry.model.ImpressionsDataType
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class DebugStrategyTest {

    @Mock
    private lateinit var taskExecutor: SplitTaskExecutor

    @Mock
    private lateinit var taskFactory: ImpressionsTaskFactory

    @Mock
    private lateinit var impressionsObserver: ImpressionsObserver

    @Mock
    private lateinit var impressionsSyncHelper: RecorderSyncHelper<KeyImpression>

    @Mock
    private lateinit var telemetryRuntimeProducer: TelemetryRuntimeProducer

    @Mock
    private lateinit var retryTimer: RetryBackoffCounterTimer

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
            retryTimer,
            30
        )
    }

    @Test
    fun `recorded impression is tracked in telemetry`() {
        strategy.apply(createUniqueImpression())

        verify(telemetryRuntimeProducer).recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 1)
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
}
