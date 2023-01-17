package io.split.android.client.service.impressions.strategy

import io.split.android.client.dtos.KeyImpression
import io.split.android.client.service.executor.SplitTaskExecutionListener
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.impressions.*
import io.split.android.client.service.synchronizer.RecorderSyncHelper
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

    private lateinit var strategy: OptimizedStrategy

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        strategy = OptimizedStrategy(
            impressionsObserver,
            impressionsCounter,
            impressionsSyncHelper,
            taskExecutor,
            taskFactory
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
        val impressionsTask = mock(ImpressionsRecorderTask::class.java)
        `when`(taskFactory.createImpressionsRecorderTask()).thenReturn(impressionsTask)
        `when`(impressionsSyncHelper.pushAndCheckIfFlushNeeded(any())).thenReturn(false)

        strategy.apply(createUniqueImpression(time = 1000000000L))

        verifyNoInteractions(taskExecutor)
    }
}
