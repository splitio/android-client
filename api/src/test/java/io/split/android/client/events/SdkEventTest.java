package io.split.android.client.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class SdkEventTest {

    @Test
    public void sdkUpdateStaticInstanceExists() {
        assertNotNull(SdkEvent.SDK_UPDATE);
    }

    @Test
    public void sdkReadyFromCacheStaticInstanceExists() {
        assertNotNull(SdkEvent.SDK_READY_FROM_CACHE);
    }

    @Test
    public void sdkReadyStaticInstanceExists() {
        assertNotNull(SdkEvent.SDK_READY);
    }

    @Test
    public void sdkReadyTimedOutStaticInstanceExists() {
        assertNotNull(SdkEvent.SDK_READY_TIMED_OUT);
    }

    @Test
    public void sdkUpdateMapsToSplitEventSdkUpdate() {
        assertEquals(SplitEvent.SDK_UPDATE, SdkEvent.SDK_UPDATE.toSplitEvent());
    }

    @Test
    public void sdkReadyFromCacheMapsToSplitEventSdkReadyFromCache() {
        assertEquals(SplitEvent.SDK_READY_FROM_CACHE, SdkEvent.SDK_READY_FROM_CACHE.toSplitEvent());
    }

    @Test
    public void sdkReadyMapsToSplitEventSdkReady() {
        assertEquals(SplitEvent.SDK_READY, SdkEvent.SDK_READY.toSplitEvent());
    }

    @Test
    public void sdkReadyTimedOutMapsToSplitEventSdkReadyTimedOut() {
        assertEquals(SplitEvent.SDK_READY_TIMED_OUT, SdkEvent.SDK_READY_TIMED_OUT.toSplitEvent());
    }
}

