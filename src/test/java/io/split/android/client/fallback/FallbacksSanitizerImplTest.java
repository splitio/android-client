package io.split.android.client.fallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class FallbacksSanitizerImplTest {

    private FallbacksSanitizerImpl mSanitizer;

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
    }

    @Test
    public void dropsInvalidFlagNamesAndTreatments() {
        Map<String, FallbackTreatment> byFlag = new HashMap<>();
        byFlag.put(VALID_FLAG, new FallbackTreatment("on"));
        byFlag.put(INVALID_FLAG_WITH_SPACE, new FallbackTreatment("off"));
        byFlag.put(LONG_101, new FallbackTreatment("off"));
        byFlag.put("tooLongTreatment", new FallbackTreatment(LONG_101));

        FallbackConfiguration config = FallbackConfiguration.builder()
                .global(new FallbackTreatment("on"))
                .byFlag(byFlag)
                .build();

        FallbackConfiguration sanitized = mSanitizer.sanitize(config);

        // only VALID_FLAG should remain
        assertEquals(1, sanitized.getByFlag().size());
        assertEquals("on", sanitized.getByFlag().get(VALID_FLAG).getTreatment());
    }

    @Test
    public void dropsInvalidGlobalTreatment() {
        FallbackConfiguration config = FallbackConfiguration.builder()
                .global(new FallbackTreatment(LONG_101)) // invalid treatment length
                .byFlag(null)
                .build();

        FallbackConfiguration sanitized = mSanitizer.sanitize(config);

        assertNull(sanitized.getGlobal());
        assertEquals(0, sanitized.getByFlag().size());
    }

    @Test
    public void byFlagTreatmentIsDroppedWhenInvalidFormat() {
        Map<String, FallbackTreatment> byFlag = new HashMap<>();
        byFlag.put(VALID_FLAG, new FallbackTreatment("on.off"));
        byFlag.put("valid_num_dot", new FallbackTreatment("123.on"));
        byFlag.put("null_treatment", new FallbackTreatment(null));

        FallbackConfiguration config = FallbackConfiguration.builder()
                .global(null)
                .byFlag(byFlag)
                .build();

        FallbackConfiguration sanitized = mSanitizer.sanitize(config);

        // Only the valid one should remain
        assertEquals(1, sanitized.getByFlag().size());
        assertEquals("123.on", sanitized.getByFlag().get("valid_num_dot").getTreatment());
    }

    @Test
    public void globalTreatmentIsDroppedWhenInvalidFormat() {
        Map<String, FallbackTreatment> byFlag = new HashMap<>();
        byFlag.put(VALID_FLAG, new FallbackTreatment("on_1-2"));
        byFlag.put("null_treatment", new FallbackTreatment(null));

        FallbackConfiguration config = FallbackConfiguration.builder()
                // Global invalid due to regex (letters cannot be followed by '.')
                .global(new FallbackTreatment("on.off"))
                .byFlag(byFlag)
                .build();

        FallbackConfiguration sanitized = mSanitizer.sanitize(config);

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

        FallbackConfiguration config = FallbackConfiguration.builder()
                .global(new FallbackTreatment("on"))
                .byFlag(byFlag)
                .build();

        FallbackConfiguration sanitized = mSanitizer.sanitize(config);

        assertEquals(2, sanitized.getByFlag().size());
        assertTrue(sanitized.getByFlag().containsKey("numWithDot"));
        assertEquals("123.on", sanitized.getByFlag().get("numWithDot").getTreatment());
        assertEquals("on_1-2", sanitized.getByFlag().get(VALID_FLAG).getTreatment());
        assertEquals("on", sanitized.getGlobal().getTreatment());
    }
}
