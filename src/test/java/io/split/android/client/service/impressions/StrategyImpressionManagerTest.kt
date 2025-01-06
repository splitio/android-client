package io.split.android.client.service.impressions

import io.split.android.client.impressions.DecoratedImpression
import io.split.android.client.impressions.Impression
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
        val impression = mock(Impression::class.java)
        val decoratedImpression = DecoratedImpression(impression, false)
        impressionManager.pushImpression(decoratedImpression)

        verify(strategy).apply(impression)
        verifyNoInteractions(noneStrategy)
    }

    @Test
    fun `pushImpression calls apply on noneStrategy when impressionsDisabled is true`() {
        val impression = mock(Impression::class.java)
        val decoratedImpression = DecoratedImpression(impression, true)
        impressionManager.pushImpression(decoratedImpression)

        verify(noneStrategy).apply(impression)
        verifyNoInteractions(strategy)
    }

    @Test
    fun `pushImpression when it is decorated uses value from impressionsDisabled to track`() {
        val impression = mock(Impression::class.java)
        val impression2 = mock(Impression::class.java)
        val decoratedImpression = DecoratedImpression(impression, false)
        val decoratedImpression2 = DecoratedImpression(impression2, true)
        impressionManager.pushImpression(decoratedImpression)
        impressionManager.pushImpression(decoratedImpression2)

        verify(strategy).apply(impression)
        verify(noneStrategy).apply(impression2)
    }

    @Test
    fun `enableTracking set to true causes impressions to be sent to strategy`() {
        impressionManager.enableTracking(true)
        val impression = mock(Impression::class.java)
        val decoratedImpression = DecoratedImpression(impression, false)
        impressionManager.pushImpression(decoratedImpression)

        verify(strategy).apply(impression)
        verifyNoInteractions(noneStrategy)
    }

    @Test
    fun `enableTracking set to false causes impressions to not be tracked`() {
        impressionManager.enableTracking(false)
        val impression = mock(Impression::class.java)
        val decoratedImpression = DecoratedImpression(impression, false)
        impressionManager.pushImpression(decoratedImpression)

        verifyNoInteractions(noneStrategy)
        verifyNoInteractions(strategy)
    }
}
