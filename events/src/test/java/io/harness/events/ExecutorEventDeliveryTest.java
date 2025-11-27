package io.harness.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ExecutorEventDeliveryTest {

    private static final long TIMEOUT_MS = 1000;

    @Test
    public void synchronousDeliveryCallsHandlerImmediately() {
        ExecutorEventDelivery<String, Void> delivery = ExecutorEventDelivery.synchronous();
        AtomicInteger callCount = new AtomicInteger(0);

        delivery.deliver((event, metadata) -> callCount.incrementAndGet(), "test", null);

        assertEquals(1, callCount.get());
    }

    @Test
    public void deliveryWithExecutorCallsHandlerOnExecutor() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ExecutorEventDelivery<String, Void> delivery = new ExecutorEventDelivery<>(executor, null);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        delivery.deliver((event, metadata) -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        }, "test", null);

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(threadName.get().startsWith("pool-"));

        executor.shutdown();
    }

    @Test
    public void deliveryWithNullExecutorUsesSynchronousDelivery() {
        ExecutorEventDelivery<String, Void> delivery = new ExecutorEventDelivery<>(null, null);
        AtomicInteger callCount = new AtomicInteger(0);
        String currentThread = Thread.currentThread().getName();
        AtomicReference<String> handlerThread = new AtomicReference<>();

        delivery.deliver((event, metadata) -> {
            callCount.incrementAndGet();
            handlerThread.set(Thread.currentThread().getName());
        }, "test", null);

        assertEquals(1, callCount.get());
        assertEquals(currentThread, handlerThread.get());
    }

    @Test
    public void deliveryIgnoresNullHandler() {
        ExecutorEventDelivery<String, Void> delivery = ExecutorEventDelivery.synchronous();

        // Should not throw
        delivery.deliver(null, "test", null);
    }

    @Test
    public void deliverySwallowsExceptions() {
        ExecutorEventDelivery<String, Void> delivery = ExecutorEventDelivery.synchronous();
        AtomicInteger secondHandlerCalls = new AtomicInteger(0);

        // First handler throws exception
        delivery.deliver((event, metadata) -> {
            throw new RuntimeException("Test exception");
        }, "test", null);

        // Second handler should still be able to execute
        delivery.deliver((event, metadata) -> secondHandlerCalls.incrementAndGet(), "test", null);

        assertEquals(1, secondHandlerCalls.get());
    }

    @Test
    public void deliveryPassesEventAndMetadata() {
        ExecutorEventDelivery<String, Integer> delivery = ExecutorEventDelivery.synchronous();
        AtomicReference<String> receivedEvent = new AtomicReference<>();
        AtomicReference<Integer> receivedMetadata = new AtomicReference<>();

        delivery.deliver((event, metadata) -> {
            receivedEvent.set(event);
            receivedMetadata.set(metadata);
        }, "testEvent", 42);

        assertEquals("testEvent", receivedEvent.get());
        assertEquals(Integer.valueOf(42), receivedMetadata.get());
    }

    @Test
    public void logsErrorWhenHandlerThrowsException() {
        TestLogging logging = new TestLogging();
        ExecutorEventDelivery<String, Void> delivery = new ExecutorEventDelivery<>(null, logging);

        delivery.deliver((event, metadata) -> {
            throw new RuntimeException("Handler failure");
        }, "testEvent", null);

        assertTrue(logging.errorMessage.contains("testEvent"));
        assertTrue(logging.errorMessage.contains("Handler failure"));
    }

    @Test
    public void doesNotLogWhenHandlerSucceeds() {
        TestLogging logging = new TestLogging();
        ExecutorEventDelivery<String, Void> delivery = new ExecutorEventDelivery<>(null, logging);

        delivery.deliver((event, metadata) -> {
            // Success - no exception
        }, "testEvent", null);

        assertNull(logging.errorMessage);
    }

    @Test
    public void worksWithNullLogging() {
        ExecutorEventDelivery<String, Void> delivery = new ExecutorEventDelivery<>(null, null);
        AtomicInteger callCount = new AtomicInteger(0);

        // Should not throw even with null logging
        delivery.deliver((event, metadata) -> {
            throw new RuntimeException("Test");
        }, "test", null);

        delivery.deliver((event, metadata) -> callCount.incrementAndGet(), "test", null);

        assertEquals(1, callCount.get());
    }
}

