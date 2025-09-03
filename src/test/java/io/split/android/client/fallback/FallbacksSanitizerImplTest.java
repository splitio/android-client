package io.split.android.client.fallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import io.split.android.client.utils.logger.LogPrinterStub;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;

public class FallbacksSanitizerImplTest {

    private FallbacksSanitizerImpl mSanitizer;
    private LogPrinterStub mLogPrinter;

    private static final String VALID_FLAG = "my_flag";
    private static final String INVALID_FLAG_WITH_SPACE = "my flag";
    private static final String LONG_101;
    static {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 101; i++) sb.append('a');
        LONG_101 = sb.toString();
    }

    @Before
    public void setUp() {
        mSanitizer = new FallbacksSanitizerImpl();
        mLogPrinter = new LogPrinterStub();
        Logger.instance().setLevel(SplitLogLevel.VERBOSE);
        Logger.instance().setPrinter(mLogPrinter);
    }

    @Test
    public void dropsInvalidFlagNamesAndTreatments() {
        Map<String, FallbackTreatment> byFlag = new HashMap<>();
        byFlag.put(VALID_FLAG, new FallbackTreatment("on"));
        byFlag.put(INVALID_FLAG_WITH_SPACE, new FallbackTreatment("off"));
        byFlag.put(LONG_101, new FallbackTreatment("off"));
        byFlag.put("tooLongTreatment", new FallbackTreatment(LONG_101));

        FallbackTreatment sanitizedGlobal = mSanitizer.sanitizeGlobal(new FallbackTreatment("on"));
        Map<String, FallbackTreatment> sanitizedByFlag = mSanitizer.sanitizeByFlag(byFlag);
        FallbackTreatmentsConfiguration sanitized = FallbackTreatmentsConfiguration.builder()
                .global(sanitizedGlobal)
                .byFlag(sanitizedByFlag)
                .build();

        Deque<String> errors = mLogPrinter.getLoggedMessages().get(android.util.Log.ERROR);
        assertTrue("Expected ERROR logs to be present", errors != null && !errors.isEmpty());
        long invalidFlagNameCount = errors.stream().filter(m -> m.contains("Invalid flag name")).count();
        assertEquals(2, invalidFlagNameCount);
        assertTrue(errors.stream().anyMatch(m -> m.contains("Discarded flag 'my flag'")));
        // invalid treatment for a specific flag name and contains the full expected message
        assertTrue(errors.stream().anyMatch(m -> m.contains("Discarded treatment for flag 'tooLongTreatment'")));
        assertTrue(errors.stream().anyMatch(m -> m.contains("Invalid treatment (max 100 chars and comply with ^[0-9]+[.a-zA-Z0-9_-]*$|^[a-zA-Z]+[a-zA-Z0-9_-]*$)")));

        assertEquals(1, sanitized.getByFlag().size());
        assertEquals("on", sanitized.getByFlag().get(VALID_FLAG).getTreatment());
    }

    @Test
    public void dropsInvalidGlobalTreatment() {
        FallbackTreatment sanitizedGlobal = mSanitizer.sanitizeGlobal(new FallbackTreatment(LONG_101)); // invalid treatment length
        Map<String, FallbackTreatment> sanitizedByFlag = mSanitizer.sanitizeByFlag(null);
        FallbackTreatmentsConfiguration sanitized = FallbackTreatmentsConfiguration.builder()
                .global(sanitizedGlobal)
                .byFlag(sanitizedByFlag)
                .build();

        // Assert error log for discarded global fallback only
        Deque<String> errors = mLogPrinter.getLoggedMessages().get(android.util.Log.ERROR);
        assertTrue("Expected ERROR logs to be present", errors != null && !errors.isEmpty());
        assertTrue(errors.stream().anyMatch(m -> m.contains("Discarded global fallback")));

        assertNull(sanitized.getGlobal());
        assertEquals(0, sanitized.getByFlag().size());
    }

    @Test
    public void byFlagTreatmentIsDroppedWhenInvalidFormat() {
        Map<String, FallbackTreatment> byFlag = new HashMap<>();
        byFlag.put(VALID_FLAG, new FallbackTreatment("on.off"));
        byFlag.put("valid_num_dot", new FallbackTreatment("123.on"));
        byFlag.put("null_treatment", new FallbackTreatment(null));

        FallbackTreatment sanitizedGlobal = mSanitizer.sanitizeGlobal(null);
        Map<String, FallbackTreatment> sanitizedByFlag = mSanitizer.sanitizeByFlag(byFlag);
        FallbackTreatmentsConfiguration sanitized = FallbackTreatmentsConfiguration.builder()
                .global(sanitizedGlobal)
                .byFlag(sanitizedByFlag)
                .build();

        // Assert error logs for invalid treatments under flags
        Deque<String> errors = mLogPrinter.getLoggedMessages().get(android.util.Log.ERROR);
        assertTrue("Expected ERROR logs to be present", errors != null && !errors.isEmpty());
        assertTrue(errors.stream().anyMatch(m -> m.contains("Discarded treatment for flag '" + VALID_FLAG + "'")));
        assertTrue(errors.stream().anyMatch(m -> m.contains("Invalid treatment (max 100 chars and comply with ^[0-9]+[.a-zA-Z0-9_-]*$|^[a-zA-Z]+[a-zA-Z0-9_-]*$)")));
        assertTrue(errors.stream().anyMatch(m -> m.contains("Discarded treatment for flag 'null_treatment'")));
        // Ensure no error for valid flag/treatment
        assertTrue(errors.stream().noneMatch(m -> m.contains("Discarded treatment for flag 'valid_num_dot'")));

        // Only the valid one should remain
        assertEquals(1, sanitized.getByFlag().size());
        assertEquals("123.on", sanitized.getByFlag().get("valid_num_dot").getTreatment());
    }

    @Test
    public void globalTreatmentIsDroppedWhenInvalidFormat() {
        Map<String, FallbackTreatment> byFlag = new HashMap<>();
        byFlag.put(VALID_FLAG, new FallbackTreatment("on_1-2"));
        byFlag.put("null_treatment", new FallbackTreatment(null));

        // Global invalid due to regex (letters cannot be followed by '.')
        FallbackTreatment sanitizedGlobal = mSanitizer.sanitizeGlobal(new FallbackTreatment("on.off"));
        Map<String, FallbackTreatment> sanitizedByFlag = mSanitizer.sanitizeByFlag(byFlag);
        FallbackTreatmentsConfiguration sanitized = FallbackTreatmentsConfiguration.builder()
                .global(sanitizedGlobal)
                .byFlag(sanitizedByFlag)
                .build();

        // Assert error logs were emitted for invalid entries
        Deque<String> errorLogs = mLogPrinter.getLoggedMessages().get(android.util.Log.ERROR);
        assertTrue("Expected ERROR logs to be present", errorLogs != null && !errorLogs.isEmpty());
        boolean hasGlobalDiscard = false;
        boolean hasNullFlagDiscard = false;
        for (String msg : errorLogs) {
            if (msg.contains("Discarded global fallback")) {
                hasGlobalDiscard = true;
            }
            if (msg.contains("Discarded treatment for flag 'null_treatment'")) {
                hasNullFlagDiscard = true;
            }
        }
        assertTrue("Expected an error about discarded global fallback", hasGlobalDiscard);
        assertTrue("Expected an error about discarded treatment for flag 'null_treatment'", hasNullFlagDiscard);

        assertNull(sanitized.getGlobal());
        // Ensure only the valid by-flag entry is preserved
        assertEquals(1, sanitized.getByFlag().size());
        assertEquals("on_1-2", sanitized.getByFlag().get(VALID_FLAG).getTreatment());
    }

    @Test
    public void validFormatTreatmentIsNotDropped() {
        Map<String, FallbackTreatment> byFlag = new HashMap<>();
        byFlag.put("numWithDot", new FallbackTreatment("123.on"));
        byFlag.put(VALID_FLAG, new FallbackTreatment("on_1-2"));

        FallbackTreatment sanitizedGlobal = mSanitizer.sanitizeGlobal(new FallbackTreatment("on"));
        Map<String, FallbackTreatment> sanitizedByFlag = mSanitizer.sanitizeByFlag(byFlag);
        FallbackTreatmentsConfiguration sanitized = FallbackTreatmentsConfiguration.builder()
                .global(sanitizedGlobal)
                .byFlag(sanitizedByFlag)
                .build();

        assertEquals(2, sanitized.getByFlag().size());
        assertTrue(sanitized.getByFlag().containsKey("numWithDot"));
        assertEquals("123.on", sanitized.getByFlag().get("numWithDot").getTreatment());
        assertEquals("on_1-2", sanitized.getByFlag().get(VALID_FLAG).getTreatment());
        assertEquals("on", sanitized.getGlobal().getTreatment());

        // No ERROR logs expected for valid-only case
        Deque<String> errors4 = mLogPrinter.getLoggedMessages().get(android.util.Log.ERROR);
        assertTrue(errors4 == null || errors4.isEmpty());
    }
}
