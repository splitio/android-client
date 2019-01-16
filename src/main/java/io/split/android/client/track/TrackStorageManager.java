package io.split.android.client.track;


import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.OnLifecycleEvent;

import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class TrackStorageManager {

    private static final String EVENTS_FILE_NAME = "SPLITIO.events.json";

    private IStorage mFileStorageManager;
    Map<String, EventsChunk> mEventsChunks;

    public TrackStorageManager(IStorage storage) {
        mFileStorageManager = storage;
        mEventsChunks = Collections.synchronizedMap(new HashMap<String, EventsChunk>());
        loadEventsFromDisk();
    }

    public boolean isEmptyCache(){
        return mEventsChunks.isEmpty();
    }

    synchronized public void deleteCachedEvents(String chunkId){
        mEventsChunks.remove(chunkId);
    }

    synchronized public void saveEvents(EventsChunk chunk){
        if (chunk == null || chunk.getEvents().isEmpty()) {
            return; // Nothing to write
        }

        mEventsChunks.put(chunk.getId(), chunk);
    }

    public List<EventsChunk> getEventsChunks() {
        return new ArrayList<>(mEventsChunks.values());
    }

    public void close(){
        saveToDisk();
    }

    private void loadEventsFromDisk(){

        try {
            String storedTracks = mFileStorageManager.read(EVENTS_FILE_NAME);
            if(Strings.isNullOrEmpty(storedTracks)) {
                return;
            }
            Type dataType = new TypeToken<Map<String, EventsChunk>>() {
            }.getType();

            Map<String, EventsChunk> chunkTracks = Json.fromJson(storedTracks, dataType);
            mEventsChunks.putAll(chunkTracks);

        } catch (IOException e) {
            Logger.e(e, "Unable to load tracks from disk: " + e.getLocalizedMessage());
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse saved tracks: " + syntaxException.getLocalizedMessage());
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private void saveToDisk() {
        try {
            String json = Json.toJson(mEventsChunks);
            mFileStorageManager.write(EVENTS_FILE_NAME, json);
        } catch (IOException e) {
            Logger.e(e, "Could not save tracks");
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse tracks to save");
        }
    }

}
