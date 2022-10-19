package io.split.android.client.service.impressions.strategy

import io.split.android.client.impressions.Impression
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.impressions.ImpressionsCounter
import io.split.android.client.service.impressions.ImpressionsObserver
import io.split.android.client.service.impressions.ImpressionsTaskFactory
import io.split.android.client.service.impressions.unique.SaveUniqueImpressionsTask
import io.split.android.client.service.impressions.unique.UniqueKeysTracker
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
    private lateinit var impressionsObserver: ImpressionsObserver

    @Mock
    private lateinit var taskExecutor: SplitTaskExecutor

    @Mock
    private lateinit var taskFactory: ImpressionsTaskFactory

    @Mock
    private lateinit var impressionsCounter: ImpressionsCounter

    @Mock
    private lateinit var uniqueKeysTracker: UniqueKeysTracker

    private lateinit var strategy: NoneStrategy

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        strategy = NoneStrategy(
            impressionsObserver,
            taskExecutor,
            taskFactory,
            impressionsCounter,
            uniqueKeysTracker
        )
    }

    @Test
    fun `keys are flushed when cache size is exceeded`() {
        `when`(uniqueKeysTracker.size()).thenReturn(30000)
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
    fun `count is not incremented when previous time does not exist`() {
        strategy.apply(createUniqueImpression())

        verifyNoInteractions(impressionsCounter)
    }

    @Test
    fun `count is incremented when previous time exists`() {
        `when`(impressionsObserver.testAndSet(any()))
            .thenReturn(null)
            .thenReturn(100L)

        strategy.apply(createUniqueImpression(split = "split"))
        strategy.apply(createUniqueImpression(split = "split"))

        verify(impressionsCounter, times(1)).inc(
            "split",
            100,
            1
        )
    }
}

private fun createUniqueImpression(split: String = UUID.randomUUID().toString()): Impression =
    Impression(
        "key",
        "bkey",
        split,
        "on",
        100L,
        "default rule",
        999L,
        null
    )
