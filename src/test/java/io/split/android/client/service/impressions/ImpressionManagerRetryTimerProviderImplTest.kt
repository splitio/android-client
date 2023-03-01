package io.split.android.client.service.impressions

import io.split.android.client.service.executor.SplitTaskExecutor
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class ImpressionManagerRetryTimerProviderImplTest {

    @Mock
    private lateinit var splitTaskExecutor: SplitTaskExecutor
    private lateinit var provider: ImpressionManagerRetryTimerProviderImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        provider = ImpressionManagerRetryTimerProviderImpl(splitTaskExecutor)
    }

    @Test
    fun `instances are reused`() {
        val impressionsTimer = provider.impressionsTimer
        val impressionsTimer2 = provider.impressionsTimer

        assertEquals(impressionsTimer, impressionsTimer2)

        val impressionsCountTimer = provider.impressionsCountTimer
        val impressionsCountTimer2 = provider.impressionsCountTimer

        assertEquals(impressionsCountTimer, impressionsCountTimer2)

        val uniqueKeysTimer = provider.uniqueKeysTimer
        val uniqueKeysTimer2 = provider.uniqueKeysTimer

        assertEquals(uniqueKeysTimer, uniqueKeysTimer2)
    }
}
