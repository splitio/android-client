package io.harness.events;

public interface EventsManager<E, I, M> {

    void register(E event, EventHandler<E, M> handler);
    void notifyInternalEvent(I event, M metadata);
    boolean eventAlreadyTriggered(E event);
    void destroy();
}
