package io.split.android.client.service.impressions

import io.split.android.client.impressions.Impression
import io.split.android.client.service.impressions.tracker.PeriodicTracker
import io.split.android.client.service.impressions.strategy.ProcessStrategy
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class StrategyImpressionManagerTest {

    @Mock
    private lateinit var tracker: PeriodicTracker

    @Mock
    private lateinit var strategy: ProcessStrategy

    private lateinit var impressionManager: StrategyImpressionManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        impressionManager = StrategyImpressionManager(strategy)
    }

    @Test
    fun `flush calls flush on periodic tracker`() {
        impressionManager.flush()

        verify(tracker).flush()
    }

    @Test
    fun `startPeriodicRecording calls startPeriodicRecording on tracker`() {
        impressionManager.startPeriodicRecording()

        verify(tracker).startPeriodicRecording()
    }

    @Test
    fun `stopPeriodicRecording() calls stopPeriodicRecording() on tracker`() {
        impressionManager.stopPeriodicRecording()

        verify(tracker).stopPeriodicRecording()
    }

    @Test
    fun `pushImpression calls apply on strategy`() {
        val impression = mock(Impression::class.java)
        impressionManager.pushImpression(impression)

        verify(strategy).apply(impression)
    }
}
