package io.split.android.client.storage.events;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.storage.StoragePusher;

public interface PersistentEventsStorage extends StoragePusher<Event> {
    // Push method is defined in StoragePusher interface
    List<Event> pop(int count);
    List<Event> getCritical();
    void setActive(@NonNull List<Event> events);
    void delete(@NonNull List<Event> events);
    void deleteInvalid(long maxTimestamp);
}