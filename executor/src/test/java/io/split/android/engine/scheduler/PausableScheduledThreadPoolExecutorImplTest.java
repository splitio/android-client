package io.split.android.engine.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class PausableScheduledThreadPoolExecutorImplTest {

    private PausableScheduledThreadPoolExecutor executor;

    @Before
    public void setUp() {
        executor = PausableScheduledThreadPoolExecutorImpl.newSingleThreadScheduledExecutor(Executors.defaultThreadFactory());
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void rejectedExecutionExceptionIsNotThrownWhenSubmittingTaskAfterShutdown() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(2);
        boolean exceptionThrown = false;
        Runnable testTask = latch::countDown;
        Runnable testTask2 = latch2::countDown;

        executor.submit(testTask);
        boolean taskIsExecuted = latch.await(2L, TimeUnit.SECONDS);

        executor.shutdownNow();
        try {
            executor.submit(testTask2);
        } catch (RejectedExecutionException exception) {
            exceptionThrown = true;
        }
        boolean task2IsExecuted = latch2.await(2L, TimeUnit.SECONDS);

        assertTrue(taskIsExecuted);
        assertFalse(task2IsExecuted);
        assertFalse(exceptionThrown);
    }

    @Test
    public void pausingWorksCorrectly() throws InterruptedException {
        final CountDownLatch timerLatch = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(4);

        Runnable testTask = latch::countDown;

        executor.scheduleAtFixedRate(testTask, 0L, 10L, TimeUnit.MILLISECONDS);
        timerLatch.await(20L, TimeUnit.MILLISECONDS);
        executor.pause();

        boolean taskCompleted = latch.await(50L, TimeUnit.MILLISECONDS);

        assertFalse(taskCompleted);
    }

    @Test
    public void resumingWorksCorrectly() throws InterruptedException {
        final CountDownLatch timerLatch = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(4);

        Runnable testTask = latch::countDown;

        executor.scheduleAtFixedRate(testTask, 0L, 10L, TimeUnit.MILLISECONDS);
        timerLatch.await(20L, TimeUnit.MILLISECONDS);
        executor.pause();
        timerLatch.await(20L, TimeUnit.MILLISECONDS);

        executor.resume();
        boolean taskCompleted = latch.await(50L, TimeUnit.MILLISECONDS);

        assertTrue(taskCompleted);
    }
}
