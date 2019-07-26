package io.split.android.client.track;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.split.android.client.dtos.Event;

public class EventsChunk {
    private String id;
    private List<Event> events;
    private int attempt = 0;

    public EventsChunk(List<Event> events) {
        this.id = UUID.randomUUID().toString();
        this.events = events;
    }

    public EventsChunk(String id, int currentAttempt) {
        this.id = id;
        this.attempt = currentAttempt;
        this.events = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public List<Event> getEvents() {
        return events;
    }

    public int getAttempt() {
        return attempt;
    }

    public void addAtempt() {
        attempt++;
    }

    public void addEvents(List<Event> events) {
        this.events.addAll(events);
    }

}
