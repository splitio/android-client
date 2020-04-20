package io.split.android.client.service.sseclient;

import org.junit.Assert;
import org.junit.Test;

public class ReconnectBackoffCounterTest {

    @Test
    public void base1() {
        long results[] = { 1L, 2L, 8L, 30L, 1L };
        testWithBase(1, results);
    }

    @Test
    public void base2() {
        long results[] = { 1L, 4L, 16L, 30L, 1L };
        testWithBase(1, results);
    }

    @Test
    public void base4() {
        long results[] = { 1L, 8L, 30L, 30L, 1L };
        testWithBase(1, results);
    }

    @Test
    public void base8() {
        long results[] = { 1L, 8L, 30L, 30L, 1L };
        testWithBase(1, results);
    }

    private void testWithBase(int base, long[] results) {
        ReconnectBackoffCounter counter
                = new ReconnectBackoffCounter(base);
        long v1 = counter.getNextRetryTime();
        long v2 = counter.getNextRetryTime();
        long v3 = counter.getNextRetryTime();
        long v4 = counter.getNextRetryTime();
        callRetry( counter, 2000);
        long vMax = counter.getNextRetryTime();
        counter.resetCounter();
        long vReset = counter.getNextRetryTime();

        Assert.assertEquals(1L, v1);
        Assert.assertEquals(2L, v2);
        Assert.assertEquals(4L, v3);
        Assert.assertEquals(8L, v4);
        Assert.assertEquals(30L, vMax);
        Assert.assertEquals(1L, vReset);
    }

    private void callRetry(ReconnectBackoffCounter counter, int times) {
        for (int i = 0; i < times; i++) {
            counter.getNextRetryTime();
        }
    }
}
