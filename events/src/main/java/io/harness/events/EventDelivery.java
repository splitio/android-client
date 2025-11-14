package io.harness.events;


interface EventDelivery<E, M> {

    void deliver(EventHandler eventHandler, E event, M metadata);
}
