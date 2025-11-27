package io.split.android.client.events.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.harness.events.EventHandler;
import io.harness.events.EventsManager;

public class DualExecutorRegistrationTest {

    private static final long TIMEOUT_MS = 1000;
    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private EventsManager<String, String, Void> mockEventsManager;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        mockEventsManager = mock(EventsManager.class);
    }

    @Test
    public void registerCallsEventsManagerTwice() {
        DualExecutorRegistration<String, String, Void> registration = 
                new DualExecutorRegistration<>(DIRECT_EXECUTOR, DIRECT_EXECUTOR);

        registration.register(
                mockEventsManager,
                "testEvent",
                (e, m) -> {},
                (e, m) -> {}
        );

        verify(mockEventsManager, times(2)).register(eq("testEvent"), any());
    }

    @Test
    public void registerBackgroundCallsEventsManagerOnce() {
        DualExecutorRegistration<String, String, Void> registration = 
                new DualExecutorRegistration<>(DIRECT_EXECUTOR, DIRECT_EXECUTOR);

        registration.registerBackground(mockEventsManager, "testEvent", (e, m) -> {});

        verify(mockEventsManager, times(1)).register(eq("testEvent"), any());
    }

    @Test
    public void registerMainThreadCallsEventsManagerOnce() {
        DualExecutorRegistration<String, String, Void> registration = 
                new DualExecutorRegistration<>(DIRECT_EXECUTOR, DIRECT_EXECUTOR);

        registration.registerMainThread(mockEventsManager, "testEvent", (e, m) -> {});

        verify(mockEventsManager, times(1)).register(eq("testEvent"), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void wrappedHandlersExecuteOnCorrectExecutors() throws InterruptedException {
        ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("background-thread");
            return t;
        });
        ExecutorService mainThreadExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("main-thread");
            return t;
        });

        DualExecutorRegistration<String, String, Void> registration = 
                new DualExecutorRegistration<>(backgroundExecutor, mainThreadExecutor);

        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<String> bgThreadName = new AtomicReference<>();
        AtomicReference<String> mainThreadName = new AtomicReference<>();

        ArgumentCaptor<EventHandler<String, Void>> captor = ArgumentCaptor.forClass(EventHandler.class);

        registration.register(
                mockEventsManager,
                "testEvent",
                (e, m) -> {
                    bgThreadName.set(Thread.currentThread().getName());
                    latch.countDown();
                },
                (e, m) -> {
                    mainThreadName.set(Thread.currentThread().getName());
                    latch.countDown();
                }
        );

        verify(mockEventsManager, times(2)).register(eq("testEvent"), captor.capture());

        // Invoke both captured handlers
        for (EventHandler<String, Void> handler : captor.getAllValues()) {
            handler.handle("testEvent", null);
        }

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals("background-thread", bgThreadName.get());
        assertEquals("main-thread", mainThreadName.get());

        backgroundExecutor.shutdown();
        mainThreadExecutor.shutdown();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void wrappedHandlerSwallowsExceptions() {
        DualExecutorRegistration<String, String, Void> registration = 
                new DualExecutorRegistration<>(DIRECT_EXECUTOR, DIRECT_EXECUTOR);

        AtomicInteger secondCallCount = new AtomicInteger(0);

        ArgumentCaptor<EventHandler<String, Void>> captor = ArgumentCaptor.forClass(EventHandler.class);

        registration.register(
                mockEventsManager,
                "testEvent",
                (e, m) -> { throw new RuntimeException("Test exception"); },
                (e, m) -> secondCallCount.incrementAndGet()
        );

        verify(mockEventsManager, times(2)).register(eq("testEvent"), captor.capture());

        // Invoke both handlers - first throws, second should still work
        for (EventHandler<String, Void> handler : captor.getAllValues()) {
            handler.handle("testEvent", null);
        }

        assertEquals(1, secondCallCount.get());
    }

    @Test
    public void registerIgnoresNullEventsManager() {
        DualExecutorRegistration<String, String, Void> registration = 
                new DualExecutorRegistration<>(DIRECT_EXECUTOR, DIRECT_EXECUTOR);

        // Should not throw
        registration.register(null, "testEvent", (e, m) -> {}, (e, m) -> {});
    }

    @Test
    public void registerIgnoresNullEvent() {
        DualExecutorRegistration<String, String, Void> registration = 
                new DualExecutorRegistration<>(DIRECT_EXECUTOR, DIRECT_EXECUTOR);

        // Should not throw
        registration.register(mockEventsManager, null, (e, m) -> {}, (e, m) -> {});

        verify(mockEventsManager, times(0)).register(any(), any());
    }

    @Test
    public void registerHandlesNullBackgroundCallback() {
        DualExecutorRegistration<String, String, Void> registration = 
                new DualExecutorRegistration<>(DIRECT_EXECUTOR, DIRECT_EXECUTOR);

        registration.register(mockEventsManager, "testEvent", null, (e, m) -> {});

        // Only main thread callback should be registered
        verify(mockEventsManager, times(1)).register(eq("testEvent"), any());
    }

    @Test
    public void registerHandlesNullMainThreadCallback() {
        DualExecutorRegistration<String, String, Void> registration = 
                new DualExecutorRegistration<>(DIRECT_EXECUTOR, DIRECT_EXECUTOR);

        registration.register(mockEventsManager, "testEvent", (e, m) -> {}, null);

        // Only background callback should be registered
        verify(mockEventsManager, times(1)).register(eq("testEvent"), any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsOnNullBackgroundExecutor() {
        new DualExecutorRegistration<>(null, DIRECT_EXECUTOR);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsOnNullMainThreadExecutor() {
        new DualExecutorRegistration<>(DIRECT_EXECUTOR, null);
    }
}

