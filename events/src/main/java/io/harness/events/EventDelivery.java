package io.harness.events;

/**
 * Interface for event delivery.
 *
 * @param <E> event type
 * @param <M> metadata type
 */
public interface EventDelivery<E, M> {

    void deliver(EventHandler<E, M> eventHandler, E event, M metadata);
}
