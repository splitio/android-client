package io.split.android.client.service.impressions.strategy

import io.split.android.client.impressions.Impression
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.impressions.ImpressionsCounter
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
            taskExecutor,
            taskFactory,
            impressionsCounter,
            uniqueKeysTracker,
            true
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
    fun `keys are never flushed when persistence is disabled`() {
        strategy = NoneStrategy(
            taskExecutor,
            taskFactory,
            impressionsCounter,
            uniqueKeysTracker,
            false
        )
        `when`(uniqueKeysTracker.isFull).thenReturn(true)

        strategy.run {
            apply(createUniqueImpression(split = "split"))
            apply(createUniqueImpression(split = "split"))
        }

        verifyNoInteractions(taskExecutor)
    }


    @Test
    fun `persistence can be disabled`() {
        `when`(uniqueKeysTracker.isFull).thenReturn(true)
        `when`(taskFactory.createSaveUniqueImpressionsTask(any())).thenReturn(mock(SaveUniqueImpressionsTask::class.java))

        strategy.run {
            apply(createUniqueImpression(split = "split"))
            apply(createUniqueImpression(split = "split"))
            enablePersistence(false)
            apply(createUniqueImpression(split = "split"))
            apply(createUniqueImpression(split = "split"))
            apply(createUniqueImpression(split = "split"))
        }

        verify(taskExecutor, times(2)).submit(
            any(SaveUniqueImpressionsTask::class.java),
            eq<SplitTaskExecutionListener?>(null)
        )
    }
}

fun createUniqueImpression(split: String = UUID.randomUUID().toString()): Impression =
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
