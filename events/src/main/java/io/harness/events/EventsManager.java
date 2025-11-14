package io.harness.events;

public interface EventsManager<E, I, M> {

    void register(E event, EventHandler handler);
    void notifyInternalEvent(I event, M metadata);
    void start();
    void stop();
    boolean eventAlreadyTriggered(E event);
}
