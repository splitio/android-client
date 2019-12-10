package io.split.android.client.storage.events;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.Event;

public interface PersistentEventsStorage {
    void push(@NonNull Event event);
    List<Event> pop(int count);
    List<Event> getCritical();
}