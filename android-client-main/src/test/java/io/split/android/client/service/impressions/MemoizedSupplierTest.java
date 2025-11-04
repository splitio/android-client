package io.split.android.client.service.impressions;

import static org.junit.Assert.assertEquals;

import androidx.core.util.Supplier;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class MemoizedSupplierTest {

    @Test
    public void valueIsOnlyComputedOnce() {
        final AtomicInteger callCount = new AtomicInteger(0);
        Supplier<String> originalSupplier = () -> {
            callCount.incrementAndGet();
            return "test";
        };

        MemoizedSupplier<String> memoizedSupplier = new MemoizedSupplier<>(originalSupplier);

        assertEquals("test", memoizedSupplier.get());
        assertEquals(1, callCount.get());

        assertEquals("test", memoizedSupplier.get());
        assertEquals("test", memoizedSupplier.get());
        assertEquals(1, callCount.get());
    }

    @Test
    public void valueIsOnlyComputedOnceWhenCallingFromMultipleThreads() throws InterruptedException {
        final AtomicInteger callCount = new AtomicInteger(0);
        Supplier<String> originalSupplier = () -> {
            callCount.incrementAndGet();
            return "Thread Safe";
        };

        MemoizedSupplier<String> memoizedSupplier = new MemoizedSupplier<>(originalSupplier);

        Thread thread1 = new Thread(memoizedSupplier::get);
        Thread thread2 = new Thread(memoizedSupplier::get);
        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // The value should still be computed only once
        assertEquals(1, callCount.get());
    }
}
