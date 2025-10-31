package io.split.android.client.service.splits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.storage.general.GeneralInfoStorage;

public class OutdatedSplitProxyHandlerTest {
    private static final String LATEST_SPEC = "1.3";
    private static final String PREVIOUS_SPEC = "1.2";
    private GeneralInfoStorage mockStorage;
    private OutdatedSplitProxyHandler handler;

    @Before
    public void setUp() {
        mockStorage = mock(GeneralInfoStorage.class);
        when(mockStorage.getLastProxyUpdateTimestamp()).thenReturn(0L);
        handler = new OutdatedSplitProxyHandler(LATEST_SPEC, PREVIOUS_SPEC, false, mockStorage, 1000L); // 1s interval for tests
    }

    @Test
    public void initialStateIsNoneAndUsesLatestSpec() {
        assertFalse(handler.isFallbackMode());
        assertFalse(handler.isRecoveryMode());
        assertEquals(LATEST_SPEC, handler.getCurrentSpec());
    }

    @Test
    public void proxyErrorTriggersFallbackModeAndUsesPreviousSpec() {
        handler.trackProxyError();
        assertTrue(handler.isFallbackMode());
        assertEquals(PREVIOUS_SPEC, handler.getCurrentSpec());
    }

    @Test
    public void fallbackModePersistsUntilIntervalElapses() {
        handler.trackProxyError();
        assertTrue(handler.isFallbackMode());
        // simulate a call to performProxyCheck within interval
        handler.performProxyCheck();
        assertTrue(handler.isFallbackMode());
        assertEquals(PREVIOUS_SPEC, handler.getCurrentSpec());
    }

    @Test
    public void intervalElapsedEntersRecoveryModeAndUsesLatestSpec() {
        handler.trackProxyError();
        // simulate time passing (10 seconds ago)
        long now = System.currentTimeMillis();
        when(mockStorage.getLastProxyUpdateTimestamp()).thenReturn(now - 10000L);
        // Re-create handler to force atomic long to re-read from storage
        handler = new OutdatedSplitProxyHandler(LATEST_SPEC, PREVIOUS_SPEC, false, mockStorage, 1000L);
        handler.performProxyCheck();
        assertTrue(handler.isRecoveryMode());
        assertEquals(LATEST_SPEC, handler.getCurrentSpec());
    }

    @Test
    public void recoveryModeResetsToNoneAfterResetProxyCheckTimestamp() {
        handler.trackProxyError();
        long now = System.currentTimeMillis();
        when(mockStorage.getLastProxyUpdateTimestamp()).thenReturn(now - 10000L);
        handler = new OutdatedSplitProxyHandler(LATEST_SPEC, PREVIOUS_SPEC, false, mockStorage, 1000L);
        handler.performProxyCheck();
        assertTrue(handler.isRecoveryMode());
        handler.resetProxyCheckTimestamp();
        // Simulate storage now returns 0L after reset
        when(mockStorage.getLastProxyUpdateTimestamp()).thenReturn(0L);
        handler = new OutdatedSplitProxyHandler(LATEST_SPEC, PREVIOUS_SPEC, false, mockStorage, 1000L);
        handler.performProxyCheck();
        assertFalse(handler.isFallbackMode());
        assertFalse(handler.isRecoveryMode());
        assertEquals(LATEST_SPEC, handler.getCurrentSpec());
    }

    @Test
    public void settingUpForBackgroundSyncIsAlwaysInNoneMode() {
        handler = new OutdatedSplitProxyHandler(LATEST_SPEC, PREVIOUS_SPEC, true, mockStorage, 1000L);
        handler.performProxyCheck();
        assertFalse(handler.isFallbackMode());
        assertFalse(handler.isRecoveryMode());
        assertEquals(LATEST_SPEC, handler.getCurrentSpec());
    }
}
