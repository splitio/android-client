package io.harness.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class EventsManagerMetadataTest {

    private static final long TIMEOUT_MS = 5000;

    enum ExternalEvent {
        READY_FROM_CACHE
    }

    enum InternalEvent {
        CACHE_A, CACHE_B, SYNC_A, SYNC_B
    }

    @Test
    public void requireAnyUsesGroupMetadataSource() throws InterruptedException {
        Set<InternalEvent> cacheGroup = new HashSet<>();
        cacheGroup.add(InternalEvent.CACHE_A);
        cacheGroup.add(InternalEvent.CACHE_B);

        Set<InternalEvent> syncGroup = new HashSet<>();
        syncGroup.add(InternalEvent.SYNC_A);
        syncGroup.add(InternalEvent.SYNC_B);

        EventsManagerConfig<ExternalEvent, InternalEvent> config = EventsManagerConfig.<ExternalEvent, InternalEvent>builder()
                .requireAny(ExternalEvent.READY_FROM_CACHE, cacheGroup, syncGroup)
                .metadataSource(ExternalEvent.READY_FROM_CACHE, cacheGroup, InternalEvent.CACHE_A)
                .metadataSource(ExternalEvent.READY_FROM_CACHE, syncGroup, InternalEvent.SYNC_A)
                .executionLimit(ExternalEvent.READY_FROM_CACHE, 1)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        EventsManager<ExternalEvent, InternalEvent, String> manager =
                new EventsManagerCore<>(config, (handler, event, metadata) -> {
                    handler.handle(event, metadata);
                    latch.countDown();
                });

        manager.register(ExternalEvent.READY_FROM_CACHE, (event, metadata) -> received.set(metadata));

        // Complete sync group: metadata should come from SYNC_A, not from SYNC_B (current event).
        manager.notifyInternalEvent(InternalEvent.SYNC_A, "sync-meta");
        manager.notifyInternalEvent(InternalEvent.SYNC_B, "sync-b-meta");

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals("sync-meta", received.get());
    }

    @Test
    public void requireAllUsesConfiguredMetadataSource() throws InterruptedException {
        EventsManagerConfig<ExternalEvent, InternalEvent> config = EventsManagerConfig.<ExternalEvent, InternalEvent>builder()
                .requireAll(ExternalEvent.READY_FROM_CACHE, InternalEvent.CACHE_A, InternalEvent.CACHE_B)
                .metadataSource(ExternalEvent.READY_FROM_CACHE, InternalEvent.CACHE_A)
                .executionLimit(ExternalEvent.READY_FROM_CACHE, 1)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        EventsManager<ExternalEvent, InternalEvent, String> manager =
                new EventsManagerCore<>(config, (handler, event, metadata) -> {
                    handler.handle(event, metadata);
                    latch.countDown();
                });

        manager.register(ExternalEvent.READY_FROM_CACHE, (event, metadata) -> received.set(metadata));

        // Provide metadata on CACHE_A only; CACHE_B completes the requireAll.
        manager.notifyInternalEvent(InternalEvent.CACHE_A, "cache-meta");
        manager.notifyInternalEvent(InternalEvent.CACHE_B, null);

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNotNull(received.get());
        assertEquals("cache-meta", received.get());
    }
}
