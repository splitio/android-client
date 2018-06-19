package io.split.android.client.track;


import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class TrackStorageManager {

    private static final String FILE_PREFIX = "SPLITIO.events";
    private static final String FILENAME_TEMPLATE = "%s_%d_%d.json";

    private IStorage _storage;

    public TrackStorageManager(IStorage storage) {
        _storage = storage;
    }

    public int getLastAttemp(String filename){
        int attemp = 0;

        int idxStart = filename.lastIndexOf("_");
        int idxEnd = filename.lastIndexOf(".json");

        attemp = Integer.parseInt(filename.substring(idxStart+1, idxEnd));
        return attemp;
    }

    synchronized public void deleteCachedEvents(String filename){
        _storage.delete(filename);
    }

    synchronized public void saveEvents(String events, int attemp){
        if (events == null || events.isEmpty()) {
            return; // Nothing to write
        }

        Logger.d("Track events to store: %s", events);

        String filename = String.format(FILENAME_TEMPLATE, FILE_PREFIX, System.currentTimeMillis(), attemp);

        try {
            Logger.d(String.format("Saving events at %s", filename));
            _storage.write(filename, events);
        } catch (IOException e) {
            Logger.e("Error saving events to disk", e);
        }
    }

    synchronized public void saveEvents(List<Event> events, int attemp){
        if (events == null || events.isEmpty()) {
            return; // Nothing to write
        }

        String entity = Json.toJson(events);
        this.saveEvents(entity, attemp);
    }

    public List<String> getAllChunkIds() {
        List<String> names = Lists.newArrayList(_storage.getAllIds());
        List<String> chunkIds = Lists.newArrayList();

        for (String name :
                names) {
            if (name.startsWith(FILE_PREFIX)) {
                chunkIds.add(name);
            }
        }

        List<String> resultChunkIds = Lists.newArrayList(chunkIds);
        return resultChunkIds;
    }

    public String readCachedEvents(String filename) {
        try {
            return _storage.read(filename);
        } catch (IOException e) {
            Logger.e(e, "Could not read chunk %s", filename);
        }
        return null;
    }

}
