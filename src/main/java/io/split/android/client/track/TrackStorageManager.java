package io.split.android.client.track;


import java.io.IOException;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class TrackStorageManager {

    private static final String FILE_PREFIX = "SPLITIO.events";

    private IStorage _storage;

    public TrackStorageManager(IStorage storage) {
        _storage = storage;
    }

    synchronized public void saveEvents(List<Event> events){
        if (events == null || events.isEmpty()) {
            return; // Nothing to write
        }

        String entity = Json.toJson(events);
        Logger.d("Track events to store: %s", entity);

        String filename = String.format("%s_%d_0.json", FILE_PREFIX, System.currentTimeMillis());

        try {
            Logger.d(String.format("Saving events at %s", filename));
            _storage.write(filename, entity);
        } catch (IOException e) {
            Logger.e("Error saving events to disk", e);
        }

    }
}
