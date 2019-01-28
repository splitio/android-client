package io.split.android.client.track;

import org.apache.http.entity.StringEntity;

import java.util.List;
import java.util.UUID;

import io.split.android.client.dtos.Event;
import io.split.android.client.utils.Utils;

public class EventsChunk {
    private String id;
    private List<Event> events;
    private int attempt = 0;

    public EventsChunk(List<Event> events) {
        this.id = UUID.randomUUID().toString();
        this.events = events;
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

    public StringEntity asJSONEntity() {
        return Utils.toJsonEntity(events);
    }
}
