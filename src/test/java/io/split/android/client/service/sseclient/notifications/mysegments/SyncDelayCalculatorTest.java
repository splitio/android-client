package io.split.android.client.service.sseclient.notifications.mysegments;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

public class SyncDelayCalculatorTest {

    @Test
    public void delayIsNotLowerThan0() {
        SyncDelayCalculator calculator = new SyncDelayCalculatorImpl();
        long delay = calculator.calculateSyncDelay(getRandomString(), 600L, 1525);
        assertTrue(delay >= 0);
    }

    @Test
    public void delayIsNotHigherThanUpdateInterval() {
        SyncDelayCalculator calculator = new SyncDelayCalculatorImpl();
        long delay = calculator.calculateSyncDelay(getRandomString(), 600L, 24515);
        assertTrue(delay <= 600L);
    }

    private String getRandomString() {
        return String.valueOf(new Random().nextInt());
    }
}
