package io.harness.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class EventsManagerTest {

    private static final long TIMEOUT_MS = 5000;
    private static final EventDelivery<CookingEvent, Void> SIMPLE_DELIVERY = (handler, event, metadata) -> handler.handle(event, metadata);

    /**
     * External events emitted to consumers.
     * <p>
     * Dependencies:
     * - DISH_SERVED: requires ALL of (INGREDIENTS_PREPPED, SEASONING_ADDED, OVEN_PREHEATED). Fires once.
     * <p>
     * - LEFTOVERS_HEATED: requires ALL of (LEFTOVER_MEAT_FOUND, LEFTOVER_VEGGIES_FOUND, LEFTOVER_SAUCE_FOUND, PLATES_RETRIEVED). Fires once.
     * <p>
     * - SEASONING_ADJUSTED: requires ANY of (SEASONING_ADDED). Prerequisite: DISH_SERVED. Fires unlimited times.
     * <p>
     * - ORDER_TIMED_OUT: requires ANY of (TIMEOUT_REACHED). Suppressed by: DISH_SERVED. Fires once.
     */
    enum CookingEvent {
        DISH_SERVED, LEFTOVERS_HEATED, SEASONING_ADJUSTED, ORDER_TIMED_OUT,
    }

    /**
     * Internal activities that trigger external events.
     */
    enum KitchenActivity {
        INGREDIENTS_PREPPED, SEASONING_ADDED, OVEN_PREHEATED, LEFTOVER_MEAT_FOUND,
        LEFTOVER_VEGGIES_FOUND, LEFTOVER_SAUCE_FOUND, PLATES_RETRIEVED, TIMEOUT_REACHED,
    }

    @Test
    public void dishServedFiresOnceAndReplaysToLateSubscribers() throws InterruptedException {
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAll(CookingEvent.DISH_SERVED, KitchenActivity.INGREDIENTS_PREPPED, KitchenActivity.SEASONING_ADDED, KitchenActivity.OVEN_PREHEATED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger h1CallCount = new AtomicInteger(0);
        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            latch.countDown();
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);

        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> h1CallCount.incrementAndGet());

        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);
        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);
        eventsManager.notifyInternalEvent(KitchenActivity.OVEN_PREHEATED, null);

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, h1CallCount.get());
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));

        // Late subscriber should receive replay
        AtomicInteger h2CallCount = new AtomicInteger(0);
        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> h2CallCount.incrementAndGet());

        assertEquals(1, h2CallCount.get());
        assertEquals(1, h1CallCount.get()); // Original handler not called again
    }

    @Test
    public void leftoversHeatedFiresOnceWhenAllLeftoversFound() throws InterruptedException {
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAll(CookingEvent.LEFTOVERS_HEATED, KitchenActivity.LEFTOVER_MEAT_FOUND, KitchenActivity.LEFTOVER_VEGGIES_FOUND, KitchenActivity.LEFTOVER_SAUCE_FOUND, KitchenActivity.PLATES_RETRIEVED)
                .executionLimit(CookingEvent.LEFTOVERS_HEATED, 1)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger hCount = new AtomicInteger(0);
        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            latch.countDown();
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);

        eventsManager.register(CookingEvent.LEFTOVERS_HEATED, (event, metadata) -> hCount.incrementAndGet());

        eventsManager.notifyInternalEvent(KitchenActivity.LEFTOVER_MEAT_FOUND, null);
        eventsManager.notifyInternalEvent(KitchenActivity.LEFTOVER_VEGGIES_FOUND, null);
        eventsManager.notifyInternalEvent(KitchenActivity.LEFTOVER_SAUCE_FOUND, null);
        eventsManager.notifyInternalEvent(KitchenActivity.PLATES_RETRIEVED, null);

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, hCount.get());
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.LEFTOVERS_HEATED));
    }

    @Test
    public void seasoningAdjustedIsEmittedOnlyAfterDishServed() throws InterruptedException {
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAll(CookingEvent.DISH_SERVED, KitchenActivity.INGREDIENTS_PREPPED, KitchenActivity.OVEN_PREHEATED, KitchenActivity.LEFTOVER_SAUCE_FOUND)
                .requireAny(CookingEvent.SEASONING_ADJUSTED, KitchenActivity.SEASONING_ADDED)
                .prerequisite(CookingEvent.SEASONING_ADJUSTED, CookingEvent.DISH_SERVED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .executionLimit(CookingEvent.SEASONING_ADJUSTED, -1)
                .build();

        CountDownLatch seasoningLatch = new CountDownLatch(1);
        AtomicInteger hCount = new AtomicInteger(0);
        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            if (event == CookingEvent.SEASONING_ADJUSTED) {
                seasoningLatch.countDown();
            }
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);

        eventsManager.register(CookingEvent.SEASONING_ADJUSTED, (event, metadata) -> hCount.incrementAndGet());

        // Trigger DISH_SERVED first (without SEASONING_ADDED)
        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);
        eventsManager.notifyInternalEvent(KitchenActivity.LEFTOVER_SAUCE_FOUND, null);
        eventsManager.notifyInternalEvent(KitchenActivity.OVEN_PREHEATED, null);

        // Wait for DISH_SERVED to be processed
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));

        // SEASONING_ADJUSTED should NOT have fired yet (no SEASONING_ADDED)
        assertEquals(0, hCount.get());

        // Now SEASONING_ADDED should trigger SEASONING_ADJUSTED (prerequisite is met)
        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);

        assertTrue(seasoningLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, hCount.get());
    }

    @Test
    public void seasoningAdjustedDoesNotReplayToLateSubscribers() throws InterruptedException {
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAll(CookingEvent.DISH_SERVED, KitchenActivity.LEFTOVER_SAUCE_FOUND)
                .requireAny(CookingEvent.SEASONING_ADJUSTED, KitchenActivity.SEASONING_ADDED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .executionLimit(CookingEvent.SEASONING_ADJUSTED, -1)
                .build();

        CountDownLatch firstSeasoningLatch = new CountDownLatch(1);
        CountDownLatch secondSeasoningLatch = new CountDownLatch(2);
        AtomicInteger h1Count = new AtomicInteger(0);
        AtomicInteger h2Count = new AtomicInteger(0);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            if (event == CookingEvent.SEASONING_ADJUSTED) {
                firstSeasoningLatch.countDown();
                secondSeasoningLatch.countDown();
            }
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);

        // Emit DISH_SERVED
        eventsManager.notifyInternalEvent(KitchenActivity.LEFTOVER_SAUCE_FOUND, null);

        eventsManager.register(CookingEvent.SEASONING_ADJUSTED, (event, metadata) -> h1Count.incrementAndGet());

        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);
        assertTrue(firstSeasoningLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, h1Count.get());

        // Late subscriber should NOT receive replay for unlimited events
        eventsManager.register(CookingEvent.SEASONING_ADJUSTED, (event, metadata) -> h2Count.incrementAndGet());
        assertEquals(0, h2Count.get());

        // Both handlers invoked on next event
        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);
        assertTrue(secondSeasoningLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(2, h1Count.get());
        assertEquals(1, h2Count.get());
    }

    @Test
    public void orderTimedOutIsSuppressedWhenDishServedFires() throws InterruptedException {
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAll(CookingEvent.DISH_SERVED, KitchenActivity.INGREDIENTS_PREPPED, KitchenActivity.SEASONING_ADDED, KitchenActivity.OVEN_PREHEATED)
                .requireAny(CookingEvent.ORDER_TIMED_OUT, KitchenActivity.TIMEOUT_REACHED)
                .suppressedBy(CookingEvent.ORDER_TIMED_OUT, CookingEvent.DISH_SERVED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .executionLimit(CookingEvent.ORDER_TIMED_OUT, 1)
                .build();

        CountDownLatch dishServedLatch = new CountDownLatch(1);
        AtomicInteger timeoutCount = new AtomicInteger(0);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            if (event == CookingEvent.DISH_SERVED) {
                dishServedLatch.countDown();
            }
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);

        eventsManager.register(CookingEvent.ORDER_TIMED_OUT, (event, metadata) -> timeoutCount.incrementAndGet());
        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> {});

        // Fire DISH_SERVED first
        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);
        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);
        eventsManager.notifyInternalEvent(KitchenActivity.OVEN_PREHEATED, null);

        assertTrue(dishServedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));

        // ORDER_TIMED_OUT should be suppressed
        eventsManager.notifyInternalEvent(KitchenActivity.TIMEOUT_REACHED, null);

        assertEquals(0, timeoutCount.get());
        assertFalse(eventsManager.eventAlreadyTriggered(CookingEvent.ORDER_TIMED_OUT));
    }

    @Test
    public void orderTimedOutFiresWhenDishServedHasNotFired() throws InterruptedException {
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAll(CookingEvent.DISH_SERVED, KitchenActivity.INGREDIENTS_PREPPED, KitchenActivity.SEASONING_ADDED, KitchenActivity.OVEN_PREHEATED)
                .requireAny(CookingEvent.ORDER_TIMED_OUT, KitchenActivity.TIMEOUT_REACHED)
                .suppressedBy(CookingEvent.ORDER_TIMED_OUT, CookingEvent.DISH_SERVED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .executionLimit(CookingEvent.ORDER_TIMED_OUT, 1)
                .build();

        CountDownLatch timeoutLatch = new CountDownLatch(1);
        AtomicInteger timeoutCount = new AtomicInteger(0);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            if (event == CookingEvent.ORDER_TIMED_OUT) {
                timeoutLatch.countDown();
            }
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);

        eventsManager.register(CookingEvent.ORDER_TIMED_OUT, (event, metadata) -> timeoutCount.incrementAndGet());

        // Trigger timeout before DISH_SERVED
        eventsManager.notifyInternalEvent(KitchenActivity.TIMEOUT_REACHED, null);

        assertTrue(timeoutLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, timeoutCount.get());
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.ORDER_TIMED_OUT));
    }

    @Test
    public void unregisterRemovesAllHandlersForEvent() throws InterruptedException {
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, KitchenActivity.INGREDIENTS_PREPPED)
                .executionLimit(CookingEvent.DISH_SERVED, -1)
                .build();

        CountDownLatch firstLatch = new CountDownLatch(2);
        CountDownLatch reRegisterLatch = new CountDownLatch(1);
        AtomicInteger h1Count = new AtomicInteger(0);
        AtomicInteger h2Count = new AtomicInteger(0);

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, SIMPLE_DELIVERY);

        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> {
            h1Count.incrementAndGet();
            firstLatch.countDown();
        });
        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> {
            h2Count.incrementAndGet();
            firstLatch.countDown();
        });

        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);
        assertTrue(firstLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, h1Count.get());
        assertEquals(1, h2Count.get());

        // Unregister all handlers for DISH_SERVED
        eventsManager.unregister(CookingEvent.DISH_SERVED);

        // Fire again - no handlers should be called
        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);

        // Use eventAlreadyTriggered to wait for processing
        eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED);

        assertEquals(1, h1Count.get());
        assertEquals(1, h2Count.get());

        // Re-register and verify it works
        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> {
            h1Count.incrementAndGet();
            reRegisterLatch.countDown();
        });
        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);

        assertTrue(reRegisterLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(2, h1Count.get());
    }

    @Test
    public void registerIsIgnoredAfterDestroy() throws InterruptedException {
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, KitchenActivity.INGREDIENTS_PREPPED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger hCount = new AtomicInteger(0);

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, SIMPLE_DELIVERY);

        // Register initial handler and trigger event for late subscribers
        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> latch.countDown());
        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));

        eventsManager.destroy();

        // Register after destroy - should be ignored, no replay
        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> hCount.incrementAndGet());

        assertEquals(0, hCount.get());
    }

    @Test
    public void notifyInternalEventIsIgnoredAfterDestroy() throws InterruptedException {
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, KitchenActivity.INGREDIENTS_PREPPED)
                .executionLimit(CookingEvent.DISH_SERVED, -1)
                .build();

        AtomicInteger hCount = new AtomicInteger(0);

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, SIMPLE_DELIVERY);

        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> hCount.incrementAndGet());

        eventsManager.destroy();

        // Notify after destroy - should be ignored
        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);

        assertEquals(0, hCount.get());
    }

    @Test
    public void eventAlreadyTriggeredReturnsFalseAfterDestroy() throws InterruptedException {
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, KitchenActivity.INGREDIENTS_PREPPED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .build();

        CountDownLatch latch = new CountDownLatch(1);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            latch.countDown();
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);

        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> {});
        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));

        eventsManager.destroy();

        // State is cleared after destroy
        assertFalse(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));
    }

    @Test
    public void handlersAreNotCalledAfterDestroy() throws InterruptedException {
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAll(CookingEvent.DISH_SERVED, KitchenActivity.INGREDIENTS_PREPPED, KitchenActivity.SEASONING_ADDED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .build();

        AtomicInteger hCount = new AtomicInteger(0);

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, SIMPLE_DELIVERY);

        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> hCount.incrementAndGet());

        // Partially satisfy requirements
        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);

        // Destroy before completing requirements
        eventsManager.destroy();

        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);

        assertEquals(0, hCount.get());
    }

    @Test
    public void eventAlreadyTriggeredRespectsExecutionLimits() throws InterruptedException {
        // Config with both one-shot (DISH_SERVED) and unlimited (SEASONING_ADJUSTED) events
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, KitchenActivity.INGREDIENTS_PREPPED)
                .requireAny(CookingEvent.SEASONING_ADJUSTED, KitchenActivity.SEASONING_ADDED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .executionLimit(CookingEvent.SEASONING_ADJUSTED, -1)
                .build();

        CountDownLatch latch = new CountDownLatch(2);
        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            latch.countDown();
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);

        // Before any triggers
        assertFalse(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));
        assertFalse(eventsManager.eventAlreadyTriggered(CookingEvent.SEASONING_ADJUSTED));

        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> {});
        eventsManager.register(CookingEvent.SEASONING_ADJUSTED, (event, metadata) -> {});

        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);
        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // One-shot event returns true (completed all executions)
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));
        // Unlimited event returns false (can still fire again)
        assertFalse(eventsManager.eventAlreadyTriggered(CookingEvent.SEASONING_ADJUSTED));
    }


    @Test
    public void requireAnyWithGroupsFiresWhenFirstGroupComplete() throws InterruptedException {
        // External event fires when EITHER:
        // Group 1: INGREDIENTS_PREPPED AND SEASONING_ADDED
        // OR
        // Group 2: LEFTOVER_MEAT_FOUND AND LEFTOVER_VEGGIES_FOUND AND LEFTOVER_SAUCE_FOUND
        Set<KitchenActivity> group1 = new HashSet<>();
        group1.add(KitchenActivity.INGREDIENTS_PREPPED);
        group1.add(KitchenActivity.SEASONING_ADDED);

        Set<KitchenActivity> group2 = new HashSet<>();
        group2.add(KitchenActivity.LEFTOVER_MEAT_FOUND);
        group2.add(KitchenActivity.LEFTOVER_VEGGIES_FOUND);
        group2.add(KitchenActivity.LEFTOVER_SAUCE_FOUND);

        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, group1, group2)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger callCount = new AtomicInteger(0);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            latch.countDown();
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);
        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> callCount.incrementAndGet());

        // Complete first group
        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);
        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, callCount.get());
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));
    }

    @Test
    public void requireAnyWithGroupsFiresWhenSecondGroupComplete() throws InterruptedException {
        // Same config as above, but complete the second group instead
        Set<KitchenActivity> group1 = new HashSet<>();
        group1.add(KitchenActivity.INGREDIENTS_PREPPED);
        group1.add(KitchenActivity.SEASONING_ADDED);

        Set<KitchenActivity> group2 = new HashSet<>();
        group2.add(KitchenActivity.LEFTOVER_MEAT_FOUND);
        group2.add(KitchenActivity.LEFTOVER_VEGGIES_FOUND);

        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, group1, group2)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger callCount = new AtomicInteger(0);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            latch.countDown();
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);
        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> callCount.incrementAndGet());

        // Complete second group (not touching first group)
        eventsManager.notifyInternalEvent(KitchenActivity.LEFTOVER_MEAT_FOUND, null);
        eventsManager.notifyInternalEvent(KitchenActivity.LEFTOVER_VEGGIES_FOUND, null);

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, callCount.get());
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));
    }

    @Test
    public void requireAnyWithGroupsDoesNotFireWithPartialGroup() throws InterruptedException {
        Set<KitchenActivity> group1 = new HashSet<>();
        group1.add(KitchenActivity.INGREDIENTS_PREPPED);
        group1.add(KitchenActivity.SEASONING_ADDED);
        group1.add(KitchenActivity.OVEN_PREHEATED);

        Set<KitchenActivity> group2 = new HashSet<>();
        group2.add(KitchenActivity.LEFTOVER_MEAT_FOUND);
        group2.add(KitchenActivity.LEFTOVER_VEGGIES_FOUND);

        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, group1, group2)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .build();

        AtomicInteger callCount = new AtomicInteger(0);

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, SIMPLE_DELIVERY);
        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> callCount.incrementAndGet());

        // Partial completion of group 1 (missing OVEN_PREHEATED)
        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);
        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);

        // Partial completion of group 2 (missing LEFTOVER_VEGGIES_FOUND)
        eventsManager.notifyInternalEvent(KitchenActivity.LEFTOVER_MEAT_FOUND, null);

        // Wait for processing to complete
        eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED);

        assertEquals(0, callCount.get());
        assertFalse(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));
    }

    @Test
    public void requireAnyWithGroupsFiresOnceEvenWhenMultipleGroupsComplete() throws InterruptedException {
        Set<KitchenActivity> group1 = new HashSet<>();
        group1.add(KitchenActivity.INGREDIENTS_PREPPED);

        Set<KitchenActivity> group2 = new HashSet<>();
        group2.add(KitchenActivity.LEFTOVER_MEAT_FOUND);

        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, group1, group2)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger callCount = new AtomicInteger(0);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            latch.countDown();
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);
        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> callCount.incrementAndGet());

        // Complete first group
        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Now complete second group as well
        eventsManager.notifyInternalEvent(KitchenActivity.LEFTOVER_MEAT_FOUND, null);

        // Wait for processing
        eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED);

        // Should only fire once due to execution limit
        assertEquals(1, callCount.get());
    }

    @Test
    public void requireAnyGroupedWithPrerequisite() throws InterruptedException {
        // DISH_SERVED requires simple condition
        // SEASONING_ADJUSTED uses OR-of-ANDs and requires DISH_SERVED first
        Set<KitchenActivity> group1 = new HashSet<>();
        group1.add(KitchenActivity.SEASONING_ADDED);

        Set<KitchenActivity> group2 = new HashSet<>();
        group2.add(KitchenActivity.LEFTOVER_MEAT_FOUND);
        group2.add(KitchenActivity.LEFTOVER_VEGGIES_FOUND);

        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, KitchenActivity.OVEN_PREHEATED)
                .requireAny(CookingEvent.SEASONING_ADJUSTED, group1, group2)
                .prerequisite(CookingEvent.SEASONING_ADJUSTED, CookingEvent.DISH_SERVED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .executionLimit(CookingEvent.SEASONING_ADJUSTED, 1)
                .build();

        CountDownLatch seasoningLatch = new CountDownLatch(1);
        AtomicInteger seasoningCount = new AtomicInteger(0);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            if (event == CookingEvent.SEASONING_ADJUSTED) {
                seasoningLatch.countDown();
            }
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);
        eventsManager.register(CookingEvent.SEASONING_ADJUSTED, (event, metadata) -> seasoningCount.incrementAndGet());

        // Complete group 2 for SEASONING_ADJUSTED, but DISH_SERVED not fired yet
        eventsManager.notifyInternalEvent(KitchenActivity.LEFTOVER_MEAT_FOUND, null);
        eventsManager.notifyInternalEvent(KitchenActivity.LEFTOVER_VEGGIES_FOUND, null);

        // Wait and verify SEASONING_ADJUSTED not fired
        eventsManager.eventAlreadyTriggered(CookingEvent.SEASONING_ADJUSTED);
        assertEquals(0, seasoningCount.get());

        // Now trigger DISH_SERVED
        eventsManager.notifyInternalEvent(KitchenActivity.OVEN_PREHEATED, null);

        // Wait for DISH_SERVED
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));

        // Now trigger something that completes group 1
        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);

        assertTrue(seasoningLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, seasoningCount.get());
    }

    @Test
    public void requireAnyGroupedWithSuppressor() throws InterruptedException {
        Set<KitchenActivity> group1 = new HashSet<>();
        group1.add(KitchenActivity.TIMEOUT_REACHED);

        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, KitchenActivity.OVEN_PREHEATED)
                .requireAny(CookingEvent.ORDER_TIMED_OUT, group1)
                .suppressedBy(CookingEvent.ORDER_TIMED_OUT, CookingEvent.DISH_SERVED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .executionLimit(CookingEvent.ORDER_TIMED_OUT, 1)
                .build();

        AtomicInteger timeoutCount = new AtomicInteger(0);

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, SIMPLE_DELIVERY);
        eventsManager.register(CookingEvent.ORDER_TIMED_OUT, (event, metadata) -> timeoutCount.incrementAndGet());

        // Trigger DISH_SERVED first
        eventsManager.notifyInternalEvent(KitchenActivity.OVEN_PREHEATED, null);
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));

        // Now trigger timeout - should be suppressed
        eventsManager.notifyInternalEvent(KitchenActivity.TIMEOUT_REACHED, null);

        // Wait for processing
        eventsManager.eventAlreadyTriggered(CookingEvent.ORDER_TIMED_OUT);

        assertEquals(0, timeoutCount.get());
        assertFalse(eventsManager.eventAlreadyTriggered(CookingEvent.ORDER_TIMED_OUT));
    }

    @Test
    public void prerequisiteChainResolvedInSingleNotification() throws InterruptedException {
        // DISH_SERVED fires when OVEN_PREHEATED
        // SEASONING_ADJUSTED fires when OVEN_PREHEATED, but requires DISH_SERVED first
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, KitchenActivity.OVEN_PREHEATED)
                .requireAny(CookingEvent.SEASONING_ADJUSTED, KitchenActivity.OVEN_PREHEATED)
                .prerequisite(CookingEvent.SEASONING_ADJUSTED, CookingEvent.DISH_SERVED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .executionLimit(CookingEvent.SEASONING_ADJUSTED, 1)
                .build();

        CountDownLatch bothFiredLatch = new CountDownLatch(2);
        AtomicInteger dishServedCount = new AtomicInteger(0);
        AtomicInteger seasoningCount = new AtomicInteger(0);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            bothFiredLatch.countDown();
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);

        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> dishServedCount.incrementAndGet());
        eventsManager.register(CookingEvent.SEASONING_ADJUSTED, (event, metadata) -> seasoningCount.incrementAndGet());

        // Single notification should trigger both events (A fires, then B fires because prerequisite is now met)
        eventsManager.notifyInternalEvent(KitchenActivity.OVEN_PREHEATED, null);

        assertTrue("Both events should fire from single notification", bothFiredLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, dishServedCount.get());
        assertEquals(1, seasoningCount.get());
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.SEASONING_ADJUSTED));
    }

    @Test
    public void prerequisiteChainWithOrOfAndsGroups() throws InterruptedException {
        // DISH_SERVED = SDK_READY_FROM_CACHE (fires when sync group completes)
        // LEFTOVERS_HEATED = SDK_READY (fires when sync completes, but requires DISH_SERVED first)

        Set<KitchenActivity> syncGroup = new HashSet<>();
        syncGroup.add(KitchenActivity.INGREDIENTS_PREPPED);
        syncGroup.add(KitchenActivity.SEASONING_ADDED);

        Set<KitchenActivity> cacheGroup = new HashSet<>();
        cacheGroup.add(KitchenActivity.LEFTOVER_MEAT_FOUND);
        cacheGroup.add(KitchenActivity.LEFTOVER_VEGGIES_FOUND);

        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                // DISH_SERVED fires when either sync or cache group completes
                .requireAny(CookingEvent.DISH_SERVED, syncGroup, cacheGroup)
                // LEFTOVERS_HEATED requires the same sync events, but also DISH_SERVED as prerequisite
                .requireAll(CookingEvent.LEFTOVERS_HEATED,
                        KitchenActivity.INGREDIENTS_PREPPED,
                        KitchenActivity.SEASONING_ADDED)
                .prerequisite(CookingEvent.LEFTOVERS_HEATED, CookingEvent.DISH_SERVED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .executionLimit(CookingEvent.LEFTOVERS_HEATED, 1)
                .build();

        CountDownLatch bothFiredLatch = new CountDownLatch(2);
        AtomicInteger dishServedCount = new AtomicInteger(0);
        AtomicInteger leftoversCount = new AtomicInteger(0);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            bothFiredLatch.countDown();
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);

        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> dishServedCount.incrementAndGet());
        eventsManager.register(CookingEvent.LEFTOVERS_HEATED, (event, metadata) -> leftoversCount.incrementAndGet());

        // First sync event
        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);

        // Second sync event should trigger chain: DISH_SERVED -> LEFTOVERS_HEATED
        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);

        assertTrue("Both events should fire when sync completes", bothFiredLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, dishServedCount.get());
        assertEquals(1, leftoversCount.get());
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.LEFTOVERS_HEATED));
    }

    @Test
    public void prerequisiteLoopTerminatesWhenNoMoreEventsCanFire() throws InterruptedException {
        // Create a chain where only DISH_SERVED can fire (SEASONING_ADJUSTED requires a different trigger)
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, KitchenActivity.OVEN_PREHEATED)
                .requireAny(CookingEvent.SEASONING_ADJUSTED, KitchenActivity.SEASONING_ADDED) // Different trigger!
                .prerequisite(CookingEvent.SEASONING_ADJUSTED, CookingEvent.DISH_SERVED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .executionLimit(CookingEvent.SEASONING_ADJUSTED, 1)
                .build();

        CountDownLatch dishServedLatch = new CountDownLatch(1);
        AtomicInteger dishServedCount = new AtomicInteger(0);
        AtomicInteger seasoningCount = new AtomicInteger(0);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            if (event == CookingEvent.DISH_SERVED) {
                dishServedLatch.countDown();
            }
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);

        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> dishServedCount.incrementAndGet());
        eventsManager.register(CookingEvent.SEASONING_ADJUSTED, (event, metadata) -> seasoningCount.incrementAndGet());

        // Only DISH_SERVED should fire, loop should terminate without firing SEASONING_ADJUSTED
        eventsManager.notifyInternalEvent(KitchenActivity.OVEN_PREHEATED, null);

        assertTrue(dishServedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, dishServedCount.get());
        assertEquals(0, seasoningCount.get()); // Should NOT fire - different trigger

        // Verify processing completed (no infinite loop)
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));
        assertFalse(eventsManager.eventAlreadyTriggered(CookingEvent.SEASONING_ADJUSTED));
    }

    @Test
    public void threeLevelPrerequisiteChain() throws InterruptedException {
        // DISH_SERVED -> SEASONING_ADJUSTED -> ORDER_TIMED_OUT
        // All triggered by the same internal event
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, KitchenActivity.OVEN_PREHEATED)
                .requireAny(CookingEvent.SEASONING_ADJUSTED, KitchenActivity.OVEN_PREHEATED)
                .requireAny(CookingEvent.ORDER_TIMED_OUT, KitchenActivity.OVEN_PREHEATED)
                .prerequisite(CookingEvent.SEASONING_ADJUSTED, CookingEvent.DISH_SERVED)
                .prerequisite(CookingEvent.ORDER_TIMED_OUT, CookingEvent.SEASONING_ADJUSTED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .executionLimit(CookingEvent.SEASONING_ADJUSTED, 1)
                .executionLimit(CookingEvent.ORDER_TIMED_OUT, 1)
                .build();

        CountDownLatch allFiredLatch = new CountDownLatch(3);
        AtomicInteger dishServedCount = new AtomicInteger(0);
        AtomicInteger seasoningCount = new AtomicInteger(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            allFiredLatch.countDown();
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);

        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> dishServedCount.incrementAndGet());
        eventsManager.register(CookingEvent.SEASONING_ADJUSTED, (event, metadata) -> seasoningCount.incrementAndGet());
        eventsManager.register(CookingEvent.ORDER_TIMED_OUT, (event, metadata) -> timeoutCount.incrementAndGet());

        // Single notification should trigger all three events in chain
        eventsManager.notifyInternalEvent(KitchenActivity.OVEN_PREHEATED, null);

        assertTrue("All three events should fire from single notification", allFiredLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, dishServedCount.get());
        assertEquals(1, seasoningCount.get());
        assertEquals(1, timeoutCount.get());
    }

    /**
     * Tests that requireAny with unlimited execution only triggers when the CURRENT
     * internal event is one of the triggers, not when historical events satisfy the condition.
     * This prevents the scenario where:
     * 1. Internal event A fires (is in requireAny for unlimited event X, but prerequisite not met)
     * 2. Internal event B fires (satisfies prerequisite for X)
     * 3. X incorrectly fires because A is in the seen set (but A was the trigger, not B)
     */
    @Test
    public void requireAnyUnlimitedOnlyTriggersOnCurrentEvent() throws InterruptedException {
        // DISH_SERVED fires when OVEN_PREHEATED (one-shot, acts as prerequisite)
        // SEASONING_ADJUSTED fires when SEASONING_ADDED (unlimited, requires DISH_SERVED)
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, KitchenActivity.OVEN_PREHEATED)
                .requireAny(CookingEvent.SEASONING_ADJUSTED, KitchenActivity.SEASONING_ADDED)
                .prerequisite(CookingEvent.SEASONING_ADJUSTED, CookingEvent.DISH_SERVED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .executionLimit(CookingEvent.SEASONING_ADJUSTED, -1) // Unlimited
                .build();

        CountDownLatch dishServedLatch = new CountDownLatch(1);
        AtomicInteger seasoningCount = new AtomicInteger(0);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            if (event == CookingEvent.DISH_SERVED) {
                dishServedLatch.countDown();
            }
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);

        eventsManager.register(CookingEvent.SEASONING_ADJUSTED, (event, metadata) -> seasoningCount.incrementAndGet());
        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> {});

        // Step 1: Fire SEASONING_ADDED BEFORE DISH_SERVED
        // This adds SEASONING_ADDED to seenInternal, but SEASONING_ADJUSTED can't fire (prerequisite not met)
        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);

        // Wait for processing
        eventsManager.eventAlreadyTriggered(CookingEvent.SEASONING_ADJUSTED);
        assertEquals("SEASONING_ADJUSTED should NOT fire (prerequisite not met)", 0, seasoningCount.get());

        // Step 2: Fire OVEN_PREHEATED to trigger DISH_SERVED
        // The bug would be: SEASONING_ADDED is in seenInternal, so SEASONING_ADJUSTED incorrectly fires
        eventsManager.notifyInternalEvent(KitchenActivity.OVEN_PREHEATED, null);

        assertTrue(dishServedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));

        // Wait for any async processing
        Thread.sleep(100);

        // SEASONING_ADJUSTED should NOT have fired because OVEN_PREHEATED is not in its requireAny
        assertEquals("SEASONING_ADJUSTED should NOT fire from OVEN_PREHEATED (wrong trigger)", 0, seasoningCount.get());

        // Step 3: Now fire SEASONING_ADDED again - this should trigger SEASONING_ADJUSTED
        CountDownLatch seasoningLatch = new CountDownLatch(1);
        eventsManager.register(CookingEvent.SEASONING_ADJUSTED, (event, metadata) -> seasoningLatch.countDown());

        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);

        assertTrue(seasoningLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals("SEASONING_ADJUSTED should fire when correct trigger arrives", 1, seasoningCount.get());
    }

    @Test
    public void requireAnyDoesNotRetriggerFromHistoricalEvents() throws InterruptedException {
        // Scenario: Multiple requireAny triggers for unlimited event
        // Event should only fire when one of ITS triggers fires, not when other events fire
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAny(CookingEvent.DISH_SERVED, KitchenActivity.OVEN_PREHEATED, KitchenActivity.PLATES_RETRIEVED)
                .requireAny(CookingEvent.SEASONING_ADJUSTED,
                        KitchenActivity.SEASONING_ADDED,
                        KitchenActivity.LEFTOVER_SAUCE_FOUND)
                .prerequisite(CookingEvent.SEASONING_ADJUSTED, CookingEvent.DISH_SERVED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .executionLimit(CookingEvent.SEASONING_ADJUSTED, -1)
                .build();

        AtomicInteger seasoningCount = new AtomicInteger(0);

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, SIMPLE_DELIVERY);

        eventsManager.register(CookingEvent.SEASONING_ADJUSTED, (event, metadata) -> seasoningCount.incrementAndGet());
        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> {});

        // Fire a SEASONING_ADJUSTED trigger before prerequisite is met
        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);
        assertEquals(0, seasoningCount.get());

        // Satisfy prerequisite with OVEN_PREHEATED
        eventsManager.notifyInternalEvent(KitchenActivity.OVEN_PREHEATED, null);
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));

        // SEASONING_ADJUSTED should NOT have fired (OVEN_PREHEATED is not its trigger)
        Thread.sleep(100);
        assertEquals("Historical SEASONING_ADDED should not cause trigger", 0, seasoningCount.get());

        // Fire an unrelated internal event
        eventsManager.notifyInternalEvent(KitchenActivity.LEFTOVER_MEAT_FOUND, null);
        Thread.sleep(100);
        assertEquals("Unrelated event should not cause trigger", 0, seasoningCount.get());

        // Now fire actual trigger - should work
        eventsManager.notifyInternalEvent(KitchenActivity.LEFTOVER_SAUCE_FOUND, null);
        Thread.sleep(100);
        assertEquals("Correct trigger should fire event", 1, seasoningCount.get());
    }

    @Test
    public void requireAllStillAccumulatesCorrectly() throws InterruptedException {
        EventsManagerConfig<CookingEvent, KitchenActivity> config = EventsManagerConfig.<CookingEvent, KitchenActivity>builder()
                .requireAll(CookingEvent.DISH_SERVED,
                        KitchenActivity.INGREDIENTS_PREPPED,
                        KitchenActivity.SEASONING_ADDED,
                        KitchenActivity.OVEN_PREHEATED)
                .executionLimit(CookingEvent.DISH_SERVED, 1)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger(0);

        EventDelivery<CookingEvent, Void> delivery = (handler, event, metadata) -> {
            handler.handle(event, metadata);
            latch.countDown();
        };

        EventsManager<CookingEvent, KitchenActivity, Void> eventsManager = new EventsManagerCore<>(config, delivery);
        eventsManager.register(CookingEvent.DISH_SERVED, (event, metadata) -> count.incrementAndGet());

        // Fire events in any order - should accumulate
        eventsManager.notifyInternalEvent(KitchenActivity.SEASONING_ADDED, null);
        assertFalse(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));

        eventsManager.notifyInternalEvent(KitchenActivity.INGREDIENTS_PREPPED, null);
        assertFalse(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));

        eventsManager.notifyInternalEvent(KitchenActivity.OVEN_PREHEATED, null);

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, count.get());
        assertTrue(eventsManager.eventAlreadyTriggered(CookingEvent.DISH_SERVED));
    }
}
