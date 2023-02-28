package io.split.android.client.service.impressions.strategy

import io.split.android.client.impressions.Impression
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.impressions.ImpressionsCounter
import io.split.android.client.service.impressions.ImpressionsTaskFactory
import io.split.android.client.service.impressions.unique.SaveUniqueImpressionsTask
import io.split.android.client.service.impressions.unique.UniqueKeysTracker
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.*

class NoneStrategyTest {

    @Mock
    private lateinit var taskExecutor: SplitTaskExecutor

    @Mock
    private lateinit var taskFactory: ImpressionsTaskFactory

    @Mock
    private lateinit var impressionsCounter: ImpressionsCounter

    @Mock
    private lateinit var uniqueKeysTracker: UniqueKeysTracker

    @Mock
    private lateinit var countRetryTimer: RetryBackoffCounterTimer

    @Mock
    private lateinit var uniqueKeysRetryTimer: RetryBackoffCounterTimer

    @Mock
    private lateinit var tracker: PeriodicTracker

    private lateinit var strategy: NoneStrategy

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        strategy = NoneStrategy(
            taskExecutor,
            taskFactory,
            impressionsCounter,
            uniqueKeysTracker,
            true,
            tracker
        )
    }

    @Test
    fun `keys are flushed when cache size is exceeded`() {
        `when`(uniqueKeysTracker.isFull).thenReturn(true)
        val uniqueImpressionsTask = mock(SaveUniqueImpressionsTask::class.java)
        `when`(taskFactory.createSaveUniqueImpressionsTask(any())).thenReturn(uniqueImpressionsTask)

        strategy.apply(createUniqueImpression())

        verify(taskExecutor).submit(
            eq(uniqueImpressionsTask),
            eq<SplitTaskExecutionListener?>(null)
        )
    }

    @Test
    fun `impression is tracked by unique keys tracker`() {
        with(createUniqueImpression()) {
            strategy.apply(this)
            verify(uniqueKeysTracker).track(key(), split())
        }
    }

    @Test
    fun `count is incremented`() {
        strategy.run {
            apply(createUniqueImpression(split = "split"))
            apply(createUniqueImpression(split = "split"))
        }

        verify(impressionsCounter, times(2)).inc(
            "split",
            100,
            1
        )
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

fun createUniqueImpression(
    split: String = UUID.randomUUID().toString(),
    time: Long = 100L
): Impression =
    Impression(
        "key",
        "bkey",
        split,
        "on",
        time,
        "default rule",
        999L,
        null
    )