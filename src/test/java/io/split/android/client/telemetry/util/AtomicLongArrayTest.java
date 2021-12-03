package io.split.android.client.telemetry.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.List;

public class AtomicLongArrayTest {

    @Test
    public void isThreadSafe() throws InterruptedException {
        AtomicLongArray atomicLongArray = new AtomicLongArray(4);

        Runnable runnable = () -> {
            atomicLongArray.increment(0);
            atomicLongArray.increment(1);
            atomicLongArray.increment(2);
            atomicLongArray.increment(3);
            atomicLongArray.increment(2);
            atomicLongArray.increment(2);
        };

        for (int i = 0; i < 1000000; ++i) {
            long expectedResult = 3;

            Thread t1 = new Thread(runnable);
            Thread t2 = new Thread(runnable);
            Thread t3 = new Thread(runnable);

            t1.start();
            t2.start();
            t3.start();
            t1.join();
            t2.join();
            t3.join();

            List<Long> longs = atomicLongArray.fetchAndClearAll();
            if (longs.get(0) != 3 || longs.get(1) != 3 || longs.get(2) != 9 || longs.get(3) != 3) {
                fail("endValue was " + longs.toString());
            }
        }
    }

}
