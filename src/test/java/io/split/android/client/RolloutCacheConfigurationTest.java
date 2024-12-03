package io.split.android.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RolloutCacheConfigurationTest {

    @Test
    public void defaultValues() {
        RolloutCacheConfiguration config = RolloutCacheConfiguration.builder().build();
        assertEquals(10, config.getExpiration());
        assertFalse(config.clearOnInit());
    }

    @Test
    public void expirationIsCorrectlySet() {
        RolloutCacheConfiguration.Builder builder = RolloutCacheConfiguration.builder();
        builder.expiration(1);
        RolloutCacheConfiguration config = builder.build();
        assertEquals(1, config.getExpiration());
    }

    @Test
    public void clearOnInitIsCorrectlySet() {
        RolloutCacheConfiguration.Builder builder = RolloutCacheConfiguration.builder();
        builder.clearOnInit(true);
        RolloutCacheConfiguration config = builder.build();
        assertTrue(config.clearOnInit());
    }

    @Test
    public void negativeExpirationIsSetToDefault() {
        RolloutCacheConfiguration.Builder builder = RolloutCacheConfiguration.builder();
        builder.expiration(-1);
        RolloutCacheConfiguration config = builder.build();
        assertEquals(10, config.getExpiration());
    }
}
