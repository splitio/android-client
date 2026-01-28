package io.split.android.client.service.impressions.observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ListenableLruCacheTest {

    @Test
    public void nullRemovalListenerIsAllowed() {
        ListenableLruCache<Long, Long> cache = new ListenableLruCache<>(3, null);

        for (int i = 1; i < 5; i++) {
            cache.put((long) i, (long) i);
        }

        assertEquals(1, cache.evictionCount());
    }

    @Test
    public void removalListenerIsCalledWhenEvictionHappens() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger removedCount = new AtomicInteger();
        List<Long> removedKeys = new ArrayList<Long>();
        ListenableLruCache.RemovalListener<Long> listener = key -> {
            removedKeys.add(key);
            removedCount.incrementAndGet();
            latch.countDown();
        };
        ListenableLruCache<Long, Long> cache = new ListenableLruCache<>(3, listener);

        for (int i = 1; i < 6; i++) {
            cache.put((long) i, (long) i);
        }

        boolean await = latch.await(1, TimeUnit.SECONDS);

        assertEquals(2, cache.evictionCount());
        assertEquals(1L, removedKeys.get(0).longValue());
        assertEquals(2L, removedKeys.get(1).longValue());

        assertEquals(2, removedCount.get());
        assertTrue(await);
    }
}
