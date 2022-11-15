package io.split.android.client.service.impressions.strategy

import io.split.android.client.dtos.KeyImpression
import io.split.android.client.service.executor.SplitTaskExecutor
import io.split.android.client.service.impressions.ImpressionsCounter
import io.split.android.client.service.impressions.ImpressionsObserver
import io.split.android.client.service.impressions.ImpressionsRecorderTask
import io.split.android.client.service.impressions.ImpressionsTaskFactory
import io.split.android.client.service.synchronizer.RecorderSyncHelper
import io.split.android.client.storage.impressions.PersistentImpressionsStorage
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
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
            taskFactory)
    }


    @Test
    @Throws(InterruptedException::class)
    fun pushImpressionReachQueueSizeImpOptimized() {
        for (i in 0..7) {
            strategy.apply(createUniqueImpression())
        }
        Thread.sleep(200)
        Mockito.verify<PersistentImpressionsStorage>(mImpressionsStorage, Mockito.times(8)).push(
            ArgumentMatchers.any(
                KeyImpression::class.java
            )
        )
        Mockito.verify<SplitTaskExecutor>(mTaskExecutor, Mockito.times(2)).submit(
            ArgumentMatchers.any(ImpressionsRecorderTask::class.java),
            ArgumentMatchers.any(RecorderSyncHelper::class.java)
        )
    }
}
