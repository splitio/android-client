package io.split.android.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RolloutCacheConfigurationTest {

    @Test
    public void defaultValues() {
        RolloutCacheConfiguration config = RolloutCacheConfiguration.builder().build();
        assertEquals(10, config.getExpirationDays());
        assertFalse(config.isClearOnInit());
    }

    @Test
    public void expirationIsCorrectlySet() {
        RolloutCacheConfiguration.Builder builder = RolloutCacheConfiguration.builder();
        builder.expirationDays(1);
        RolloutCacheConfiguration config = builder.build();
        assertEquals(1, config.getExpirationDays());
    }

    @Test
    public void clearOnInitIsCorrectlySet() {
        RolloutCacheConfiguration.Builder builder = RolloutCacheConfiguration.builder();
        builder.clearOnInit(true);
        RolloutCacheConfiguration config = builder.build();
        assertTrue(config.isClearOnInit());
    }

    @Test
    public void negativeExpirationIsSetToDefault() {
        RolloutCacheConfiguration.Builder builder = RolloutCacheConfiguration.builder();
        builder.expirationDays(-1);
        RolloutCacheConfiguration config = builder.build();
        assertEquals(10, config.getExpirationDays());
    }
}
