package io.harness.events;


public interface EventDelivery<E, M> {

    void deliver(EventHandler eventHandler, E event, M metadata);
}
