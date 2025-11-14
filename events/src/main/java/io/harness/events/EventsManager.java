package io.harness.events;

public interface EventsManager<E, I, M> {

    void register(E event, io.harness.events.EventHandler handler);
    void notifyInternalEvent(I event, M metadata);
    void start();
    void stop();
    boolean eventAlreadyTriggered(E event);

    interface EventHandler<E, M> extends io.harness.events.EventHandler {
        void handle(E event, M metadata);
    }
}
