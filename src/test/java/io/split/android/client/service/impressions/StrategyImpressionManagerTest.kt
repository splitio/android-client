package io.split.android.client.service.impressions

import io.split.android.client.impressions.DecoratedImpression
import io.split.android.client.service.impressions.strategy.PeriodicTracker
import io.split.android.client.service.impressions.strategy.ProcessStrategy
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class StrategyImpressionManagerTest {

    @Mock
    private lateinit var tracker: PeriodicTracker

    @Mock
    private lateinit var strategy: ProcessStrategy

    @Mock
    private lateinit var noneStrategy: ProcessStrategy

    @Mock
    private lateinit var noneTracker: PeriodicTracker

    private lateinit var impressionManager: StrategyImpressionManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        impressionManager = StrategyImpressionManager(noneStrategy, noneTracker, strategy, tracker)
    }

    @Test
    fun `flush calls flush on periodic tracker`() {
        impressionManager.flush()

        verify(tracker).flush()
        verify(noneTracker).flush()
    }

    @Test
    fun `startPeriodicRecording calls startPeriodicRecording on tracker`() {
        impressionManager.startPeriodicRecording()

        verify(tracker).startPeriodicRecording()
        verify(noneTracker).startPeriodicRecording()
    }

    @Test
    fun `stopPeriodicRecording() calls stopPeriodicRecording() on tracker`() {
        impressionManager.stopPeriodicRecording()

        verify(tracker).stopPeriodicRecording()
        verify(noneTracker).stopPeriodicRecording()
    }

    @Test
    fun `pushImpression calls apply on strategy`() {
        val impression = mock(DecoratedImpression::class.java)
        `when`(impression.trackImpressions).thenReturn(true)
        impressionManager.pushImpression(impression)

        verify(strategy).apply(impression)
        verifyNoInteractions(noneStrategy)
    }

    @Test
    fun `pushImpression calls apply on noneStrategy when trackImpressions is false`() {
        val impression = mock(DecoratedImpression::class.java)
        `when`(impression.trackImpressions).thenReturn(false)
        impressionManager.pushImpression(impression)

        verify(noneStrategy).apply(impression)
        verifyNoInteractions(strategy)
    }

    @Test
    fun `pushImpression when it is decorated uses value from trackImpression to track`() {
        val impression = mock(DecoratedImpression::class.java)
        val impression2 = mock(DecoratedImpression::class.java)
        `when`(impression.trackImpressions).thenReturn(false)
        `when`(impression2.trackImpressions).thenReturn(true)
        impressionManager.pushImpression(impression)
        impressionManager.pushImpression(impression2)

        verify(strategy).apply(impression2)
        verify(noneStrategy).apply(impression)
    }

    @Test
    fun `enableTracking set to true causes impressions to be sent to strategy`() {
        impressionManager.enableTracking(true)
        val impression = mock(DecoratedImpression::class.java)
        `when`(impression.trackImpressions).thenReturn(true)
        impressionManager.pushImpression(impression)

        verify(strategy).apply(impression)
        verifyNoInteractions(noneStrategy)
    }

    @Test
    fun `enableTracking set to false causes impressions to not be tracked`() {
        impressionManager.enableTracking(false)
        val impression = mock(DecoratedImpression::class.java)
        `when`(impression.trackImpressions).thenReturn(true)
        impressionManager.pushImpression(impression)

        verifyNoInteractions(noneStrategy)
        verifyNoInteractions(strategy)
    }
}
