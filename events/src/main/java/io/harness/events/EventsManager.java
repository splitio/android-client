package io.harness.events;

import org.jetbrains.annotations.Nullable;

/**
 * Interface for events manager.
 *
 * @param <E> external events type
 * @param <I> internal events type
 * @param <M> metadata type
 */
public interface EventsManager<E, I, M> {

    /**
     * Registers a handler to be executed when the event is triggered.
     *
     * @param event   event to register
     * @param handler handler to execute when the event is triggered
     */
    void register(E event, EventHandler<E, M> handler);

    /**
     * Unregisters all registered handlers for an event.
     *
     * @param event event to unregister handlers for
     */
    void unregister(E event);

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
