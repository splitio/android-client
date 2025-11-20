package io.harness.events;

import androidx.annotation.Nullable;

/**
 * Interface for events manager.
 *
 * @param <E> external events type
 * @param <I> internal events type
 * @param <M> metadata type
 */
public interface EventsManager<E, I, M> {

    /**
     * Registers a callback to be executed when the event is triggered.
     *
     * @param event   event to register
     * @param handler callback to execute when the event is triggered
     */
    void register(E event, EventHandler<E, M> handler);

    /**
     * Notifies an internal event has occurred.
     *
     * @param event    internal event to notify
     * @param metadata optional metadata
     */
    void notifyInternalEvent(I event, @Nullable M metadata);

    /**
     * Checks if the event has already been triggered.
     *
     * @param event event to check
     * @return whether event has been triggered
     */
    boolean eventAlreadyTriggered(E event);

    /**
     * Destroys the events manager.
     * This should be called when the events manager is no longer needed.
     */
    void destroy();
}
