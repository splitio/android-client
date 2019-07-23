package io.split.android.client.track;


import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.VisibleForTesting;

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

import io.split.android.client.dtos.Event;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class TrackStorageManager {

    private static final String CHUNK_HEADERS_FILE_NAME = "SPLITIO.events_chunk_headers.json";
    private static final String EVENTS_FILE_NAME = "SPLITIO.events_%d.json";

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
        List<ChunkHeader> headers = getChunkHeaders(mEventsChunks);


        try {
            String json = Json.toJson(headers);
            mFileStorageManager.write(CHUNK_HEADERS_FILE_NAME, json);
        } catch (IOException e) {
            Logger.e(e, "Could not save tracks headers");
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse tracks to save");
        }

        List<Map<String, List<Event>>> eventChunks = splitChunks(getEventsChunks());
        int i = 0;
        for (Map<String, List<Event>> chunk : eventChunks){
            try {
                String json = Json.toJson(chunk);
                String fileName = String.format(EVENTS_FILE_NAME, i);
                mFileStorageManager.write(fileName, json);
                i++;
            } catch (IOException e) {
                Logger.e(e, "Could not save tracks");
            } catch (JsonSyntaxException syntaxException) {
                Logger.e(syntaxException, "Unable to parse tracks to save");
            }
        }
    }

    private static final int MAX_BYTES_PER_CHUNK = 3000000; //3MB

    private List<ChunkHeader> getChunkHeaders(Map<String, EventsChunk> eventChunks) {
        List<ChunkHeader> chunkHeaders = new ArrayList<>();
        for(EventsChunk eventsChunk : eventChunks.values()) {
            ChunkHeader header = new ChunkHeader(eventsChunk.getId(), eventsChunk.getAttempt());
            chunkHeaders.add(header);
        }
        return chunkHeaders;
    }

    private List<Map<String, List<Event>>> splitChunks(List<EventsChunk> eventChunks) {

        List<Map<String, List<Event>>> splitEvents = new ArrayList<>();
        long bytesCount = 0;
        List<Event> currentEvents = new ArrayList<>();
        Map<String, List<Event>> currentChunk = new HashMap<>();
        for(EventsChunk eventsChunk : eventChunks) {
            List<Event> events = eventsChunk.getEvents();
            for(Event event : events) {
                if (bytesCount + event.getSizeInBytes() > MAX_BYTES_PER_CHUNK) {
                    currentChunk.put(eventsChunk.getId(), currentEvents);
                    splitEvents.add(currentChunk);
                    currentChunk = new HashMap<>();
                    currentEvents  = new ArrayList<>();
                    bytesCount = 0;
                }
                currentEvents.add(event);
                bytesCount +=event.getSizeInBytes();
            }
            if(currentEvents.size() > 0) {
                currentChunk.put(eventsChunk.getId(), currentEvents);
                currentEvents  = new ArrayList<>();
            }
        }
        splitEvents.add(currentChunk);
        return splitEvents;
    }

    private Map<String, List<Event>> buildDiskChunk(String chunkId, List<Event> events) {
        Map<String, List<Event>> chunk = new HashMap<>();
        chunk.put(chunkId, events);
        return chunk;
    }

    public static class ChunkHeader {
        private String id;
        private int attempts;
        public ChunkHeader(String id, int attempts) {
            this.id = id;
            this.attempts = attempts;
        }
    }

}
