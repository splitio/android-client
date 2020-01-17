package io.split.android.client.storage.db.migrator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.storage.legacy.TrackStorageManager;
import io.split.android.client.track.EventsChunk;
import io.split.android.client.utils.Json;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventsMigratorHelperImpl implements EventsMigratorHelper {
    TrackStorageManager mEventsStorageManager;

    public EventsMigratorHelperImpl(@NotNull TrackStorageManager eventsStorageManager) {
        mEventsStorageManager = checkNotNull(eventsStorageManager);
    }

    public List<EventEntity> loadLegacyEventsAsEntities() {
        List<EventsChunk> eventsChunks = mEventsStorageManager.getEventsChunks();
        List<EventEntity> entities = new ArrayList<>();
        for(EventsChunk chunk : eventsChunks) {
            List<Event> events = chunk.getEvents();
            for(Event event : events) {
                EventEntity eventEntity = createEventEntity(event);
                entities.add(eventEntity);
            }
        }
        mEventsStorageManager.deleteAllFiles();
        return entities;
    }

    private EventEntity createEventEntity(Event event) {
        EventEntity entity = new EventEntity();
        entity.setBody(Json.toJson(event));
        entity.setUpdatedAt(event.timestamp);
        entity.setStatus(StorageRecordStatus.ACTIVE);
        return entity;
    }
}
