package io.split.android.client.service.impressions.strategy

import io.split.android.client.dtos.KeyImpression
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.impressions.ImpressionsRecorderTask
import io.split.android.client.service.impressions.ImpressionsTaskFactory
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
    private lateinit var impressionsSyncHelper: RecorderSyncHelper<KeyImpression>

    @Mock
    private lateinit var telemetryRuntimeProducer: TelemetryRuntimeProducer

    @Mock
    private lateinit var tracker: PeriodicTracker

    private lateinit var strategy: DebugStrategy

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        strategy = DebugStrategy(
            impressionsSyncHelper,
            taskExecutor,
            taskFactory,
            telemetryRuntimeProducer,
            tracker
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

    @Test
    fun `flush calls flush on tracker`() {
        strategy.flush()

        verify(tracker).flush()
    }

    @Test
    fun `startPeriodicRecording calls startPeriodicRecording on tracker`() {
        strategy.startPeriodicRecording()

        verify(tracker).startPeriodicRecording()
    }

    @Test
    fun `stopPeriodicRecording calls stopPeriodicRecording on tracker`() {
        strategy.stopPeriodicRecording()

        verify(tracker).stopPeriodicRecording()
    }

    @Test
    fun `enableTracking calls enableTracking on tracker`() {
        strategy.enableTracking(true)

        verify(tracker).enableTracking(true)
    }
}
