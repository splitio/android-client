package io.harness.events;

/**
 * Interface for event handlers. This represents a callback
 * that will be executed when an event is triggered.
 *
 * @param <E> event type
 * @param <M> metadata type
 */
public interface EventHandler<E, M> {

    void handle(E event, M metadata);
}
