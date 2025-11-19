package io.split.android.client.service.sseclient.notifications.mysegments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

import io.split.android.client.service.sseclient.notifications.HashingAlgorithm;
import io.split.android.client.service.sseclient.notifications.MySegmentUpdateStrategy;

public class SyncDelayCalculatorTest {

    @Test
    public void delayIsNotLowerThan0() {
        SyncDelayCalculator calculator = new SyncDelayCalculatorImpl();
        long delay = calculator.calculateSyncDelay(getRandomString(), 600L, 1525, MySegmentUpdateStrategy.UNBOUNDED_FETCH_REQUEST, HashingAlgorithm.MURMUR3_32);
        assertTrue(delay >= 0);
    }

    @Test
    public void delayIsNotHigherThanUpdateInterval() {
        SyncDelayCalculator calculator = new SyncDelayCalculatorImpl();
        long delay = calculator.calculateSyncDelay(getRandomString(), 600L, 24515, MySegmentUpdateStrategy.BOUNDED_FETCH_REQUEST, HashingAlgorithm.MURMUR3_32);
        assertTrue(delay <= 600L);
    }

    @Test
    public void delayIsZeroWhenUpdateStrategyIsKeyList() {
        SyncDelayCalculator calculator = new SyncDelayCalculatorImpl();
        long delay = calculator.calculateSyncDelay(getRandomString(), 600L, 24515, MySegmentUpdateStrategy.KEY_LIST, HashingAlgorithm.MURMUR3_32);
        assertEquals(0, delay);
    }

    @Test
    public void delayIsZeroWhenUpdateStrategyIsSegmentRemoval() {
        SyncDelayCalculator calculator = new SyncDelayCalculatorImpl();
        long delay = calculator.calculateSyncDelay(getRandomString(), 600L, 24515, MySegmentUpdateStrategy.SEGMENT_REMOVAL, HashingAlgorithm.MURMUR3_32);
        assertEquals(0, delay);
    }

    @Test
    public void delayIsZeroWhenHashingAlgorithmIsNone() {
        SyncDelayCalculator calculator = new SyncDelayCalculatorImpl();
        long delay = calculator.calculateSyncDelay(getRandomString(), 600L, 24515, MySegmentUpdateStrategy.BOUNDED_FETCH_REQUEST, HashingAlgorithm.NONE);
        assertEquals(0, delay);
    }

    @Test
    public void delayIsLessThanSixtyMsWhenUpdateIntervalIsNull() {
        SyncDelayCalculator calculator = new SyncDelayCalculatorImpl();
        long delay = calculator.calculateSyncDelay(getRandomString(), null, 24515, MySegmentUpdateStrategy.BOUNDED_FETCH_REQUEST, HashingAlgorithm.MURMUR3_32);
        assertTrue(delay <= 60000);
    }

    @Test
    public void delayIsLessThanSixtyMsWhenUpdateIntervalIsZero() {
        SyncDelayCalculator calculator = new SyncDelayCalculatorImpl();
        long delay = calculator.calculateSyncDelay(getRandomString(), 0L, 24515, MySegmentUpdateStrategy.BOUNDED_FETCH_REQUEST, HashingAlgorithm.MURMUR3_32);
        assertTrue(delay <= 60000);
    }

    @Test
    public void delayIsLessThanSixtyMsWhenUpdateIntervalIsNegative() {
        SyncDelayCalculator calculator = new SyncDelayCalculatorImpl();
        long delay = calculator.calculateSyncDelay(getRandomString(), -1L, 24515, MySegmentUpdateStrategy.BOUNDED_FETCH_REQUEST, HashingAlgorithm.MURMUR3_32);
        assertTrue(delay <= 60000);
    }

    @Test
    public void delayIsCalculatedWithNullSeed() {
        SyncDelayCalculator calculator = new SyncDelayCalculatorImpl();
        long delay = calculator.calculateSyncDelay(getRandomString(), 1L, null, MySegmentUpdateStrategy.BOUNDED_FETCH_REQUEST, HashingAlgorithm.MURMUR3_32);
        assertTrue(delay >= 0 && delay <= 1);
    }

    private String getRandomString() {
        return String.valueOf(new Random().nextInt());
    }
}
