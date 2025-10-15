package io.split.android.client.service.splits;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.split.android.client.dtos.TargetingRulesChange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class TargetingRulesCacheTest {

    private TargetingRulesCache mCache;
    private TargetingRulesChange mMockChange;

    @Before
    public void setup() {
        mCache = new TargetingRulesCache();
        mMockChange = mock(TargetingRulesChange.class);
    }

    @Test
    public void setAndGetAndConsumeReturnsValue() {
        mCache.set(mMockChange);

        TargetingRulesChange result = mCache.getAndConsume();

        assertEquals(mMockChange, result);
    }

    @Test
    public void getAndConsumeWithoutSetReturnsNull() {
        TargetingRulesChange result = mCache.getAndConsume();

        assertNull(result);
    }

    @Test
    public void getAndConsumeCalledTwiceReturnsNullSecondTime() {
        mCache.set(mMockChange);

        TargetingRulesChange firstResult = mCache.getAndConsume();
        TargetingRulesChange secondResult = mCache.getAndConsume();

        assertEquals(mMockChange, firstResult);
        assertNull(secondResult);
    }

    @Test
    public void setAfterConsumptionDoesNotStoreValue() {
        mCache.set(mMockChange);
        mCache.getAndConsume();

        TargetingRulesChange newMockChange = mock(TargetingRulesChange.class);
        mCache.set(newMockChange);
        TargetingRulesChange result = mCache.getAndConsume();

        assertNull(result);
    }

    @Test
    public void hasValueWithValueReturnsTrue() {
        mCache.set(mMockChange);

        assertTrue(mCache.hasValue());
    }

    @Test
    public void hasValueWithoutValueReturnsFalse() {
        assertFalse(mCache.hasValue());
    }

    @Test
    public void hasValueAfterConsumptionReturnsFalse() {
        mCache.set(mMockChange);
        mCache.getAndConsume();

        assertFalse(mCache.hasValue());
    }

    @Test
    public void setWithNullValueStoresNull() {
        mCache.set(null);

        assertFalse(mCache.hasValue());
        assertNull(mCache.getAndConsume());
    }

    @Test
    public void setWithLockExecutesOperationAndStoresResult() throws Exception {
        mCache.setWithLock(() -> mMockChange);

        TargetingRulesChange result = mCache.getAndConsume();
        assertEquals(mMockChange, result);
    }

    @Test
    public void setWithLockDoesNotStoreAfterConsumption() throws Exception {
        mCache.set(mMockChange);
        mCache.getAndConsume();

        TargetingRulesChange newMockChange = mock(TargetingRulesChange.class);
        mCache.setWithLock(() -> newMockChange);

        assertNull(mCache.getAndConsume());
    }

    @Test
    public void concurrentSetAndGetMaintainsThreadSafety() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicReference<TargetingRulesChange> consumedValue = new AtomicReference<>();
        AtomicBoolean multipleConsumptions = new AtomicBoolean(false);

        // Create setter threads
        for (int i = 0; i < threadCount / 2; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    mCache.set(mMockChange);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Create getter threads
        for (int i = 0; i < threadCount / 2; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    TargetingRulesChange result = mCache.getAndConsume();
                    if (result != null) {
                        if (!consumedValue.compareAndSet(null, result)) {
                            multipleConsumptions.set(true);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

        // Only one thread should have successfully consumed the value
        assertFalse("Multiple threads consumed the value", multipleConsumptions.get());
        assertNotNull("At least one thread should have consumed the value", consumedValue.get());
        assertEquals(mMockChange, consumedValue.get());
    }

    @Test
    public void concurrentGetAndConsumeOnlyOneThreadSucceeds() throws InterruptedException {
        mCache.set(mMockChange);

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicReference<TargetingRulesChange> consumedValue = new AtomicReference<>();
        AtomicBoolean multipleConsumptions = new AtomicBoolean(false);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    TargetingRulesChange result = mCache.getAndConsume();
                    if (result != null) {
                        if (!consumedValue.compareAndSet(null, result)) {
                            multipleConsumptions.set(true);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

        assertFalse("Multiple threads consumed the value", multipleConsumptions.get());
        assertEquals(mMockChange, consumedValue.get());
    }

    @Test
    public void setWithLockPreventsRaceConditions() throws InterruptedException {
        CountDownLatch thread1Started = new CountDownLatch(1);
        CountDownLatch thread1CanProceed = new CountDownLatch(1);
        AtomicBoolean thread2Executed = new AtomicBoolean(false);
        AtomicReference<Exception> thread1Exception = new AtomicReference<>();
        AtomicReference<Exception> thread2Exception = new AtomicReference<>();

        Thread thread1 = new Thread(() -> {
            try {
                mCache.setWithLock(() -> {
                    thread1Started.countDown();
                    thread1CanProceed.await();
                    return mMockChange;
                });
            } catch (Exception e) {
                thread1Exception.set(e);
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                thread1Started.await();
                // Try to set - should block until thread1 completes
                mCache.setWithLock(() -> {
                    thread2Executed.set(true);
                    return mock(TargetingRulesChange.class);
                });
            } catch (Exception e) {
                thread2Exception.set(e);
            }
        });

        thread1.start();
        thread2.start();

        // Wait for thread1 to start operation
        assertTrue(thread1Started.await(1, TimeUnit.SECONDS));
        
        // Give thread2 a moment to try acquiring the lock
        Thread.sleep(100);
        
        // Thread2 should not have executed yet
        assertFalse(thread2Executed.get());
        
        // Allow thread1 to proceed and release lock
        thread1CanProceed.countDown();
        
        thread1.join(1000);
        thread2.join(1000);
        
        // Now thread2 should have executed
        assertTrue(thread2Executed.get());
        assertNull(thread1Exception.get());
        assertNull(thread2Exception.get());
    }

    @Test
    public void hasValueIsThreadSafe() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicBoolean inconsistentState = new AtomicBoolean(false);

        mCache.set(mMockChange);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    boolean hasValue = mCache.hasValue();
                    TargetingRulesChange value = mCache.getAndConsume();
                    
                    // If hasValue was true but we got null, or vice versa, state is inconsistent
                    if (hasValue && value == null && !mCache.hasValue()) {
                        // This is actually OK - another thread consumed it between checks
                    } else if (!hasValue && value != null) {
                        inconsistentState.set(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

        assertFalse("Inconsistent state detected", inconsistentState.get());
    }
}
