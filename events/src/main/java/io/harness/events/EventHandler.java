package io.harness.events;

public interface EventHandler<E, M> {

    void handle(E event, M metadata);
}
