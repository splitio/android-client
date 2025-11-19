package io.split.android.client.service.events;

public class EventsRecorderTaskConfig {
    final private int eventsPerPush;
    public EventsRecorderTaskConfig(int eventsPerPush) {
        this.eventsPerPush = eventsPerPush;
    }

    public int getEventsPerPush() {
        return eventsPerPush;
    }
}
