package io.split.android.client.service;

import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.split.android.client.impressions.Impression;
import io.split.android.client.service.impressions.ImpressionsObserver;

public class ImpressionsObserverTest {

    // We allow the cache implementation to have a 0.01% drift in size when elements change, given that it's internal
    // structure/references might vary, and the ObjectSizeCalculator is not 100% accurate
    private final Random mRandom = new Random();

    private List<Impression> generateImpressions(long count) {
        ArrayList<Impression> imps = new ArrayList<>();
        for (long i = 0; i < count; i++) {
            Impression imp = new Impression(String.format("key_%d", i),
                    null,
                    String.format("feature_%d", i % 10),
                    (i % 2 == 0) ? "on" : "off",
                    System.currentTimeMillis(),
                    (i % 2 == 0) ? "in segment all" : "whitelisted",
                    i * i,
                    null);
            imps.add(imp);
        }
        return imps;
    }

    @Test
    public void testBasicFunctionality() {
        ImpressionsObserver observer = new ImpressionsObserver(5);
        Impression imp = new Impression("someKey",
                null, "someFeature",
                "on", System.currentTimeMillis(),
                "in segment all",
                1234L,
                null);

        // Add 5 new impressions so that the old one is evicted and re-try the test.
        for (Impression i : generateImpressions(5)) {
            observer.testAndSet(i);
        }
        assertNull(observer.testAndSet(imp));
        Assert.assertEquals(observer.testAndSet(imp).longValue(), imp.time());
    }

    @Test
    public void testConcurrencyVsAccuracy() throws InterruptedException {
        ImpressionsObserver observer = new ImpressionsObserver(5000);
        ConcurrentLinkedQueue<Impression> imps = new ConcurrentLinkedQueue<>();
        Thread t1 = new Thread(() -> caller(observer, 1000, imps));
        Thread t2 = new Thread(() -> caller(observer, 1000, imps));
        Thread t3 = new Thread(() -> caller(observer, 1000, imps));
        Thread t4 = new Thread(() -> caller(observer, 1000, imps));
        Thread t5 = new Thread(() -> caller(observer, 1000, imps));

        // start the 5 threads an wait for them to finish.
        t1.setDaemon(true);
        t2.setDaemon(true);
        t3.setDaemon(true);
        t4.setDaemon(true);
        t5.setDaemon(true);
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        t1.join();
        t2.join();
        t3.join();
        t4.join();
        t5.join();

        Assert.assertEquals(imps.size(), 5000);
        for (Impression i : imps) {
            Assert.assertTrue(i.previousTime() == null || i.previousTime() <= i.time());
        }
    }

    @Test
    public void testAndSetWithNullImpressionReturnsNullPreviousTime() {
        ImpressionsObserver observer = new ImpressionsObserver(1);

        assertNull(observer.testAndSet(null));
    }

    private void caller(ImpressionsObserver o, int count, ConcurrentLinkedQueue<Impression> imps) {

        while (count-- > 0) {
            Impression i = new Impression("key_" + mRandom.nextInt(100),
                    null,
                    "feature_" + mRandom.nextInt(10),
                    mRandom.nextBoolean() ? "on" : "off",
                    System.currentTimeMillis(),
                    "label" + mRandom.nextInt(5),
                    1234567L,
                    null);

            i = i.withPreviousTime(o.testAndSet(i));
            imps.offer(i);
        }
    }
}
