package io.harness.events;

/**
 * Factory class for creating {@link EventsManager} instances.
 * This class decouples the creation of the {@link EventsManager} instance from the implementation.
 */
public final class EventsManagers {

    private EventsManagers() {
        // Utility class
    }

    /**
     * Creates a new EventsManager with the given configuration and delivery mechanism.
     *
     * @param config   the configuration defining event relationships
     * @param delivery the delivery mechanism for dispatching events to handlers
     * @param <E>      external events type
     * @param <I>      internal events type
     * @param <M>      metadata type
     * @return a new EventsManager instance
     */
    public static <E, I, M> EventsManager<E, I, M> create(
            EventsManagerConfig<E, I> config,
            EventDelivery<E, M> delivery) {
        return new EventsManagerCore<>(config, delivery);
    }
}
