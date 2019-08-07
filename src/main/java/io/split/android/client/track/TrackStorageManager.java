package io.split.android.client.track;


import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;
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

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.dtos.Event;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class TrackStorageManager implements LifecycleObserver {

    private static final String LEGACY_EVENTS_FILE_NAME = "SPLITIO.events.json";
    private static final String TRACK_FILE_PREFIX = "SPLITIO.events";
    private static final String EVENTS_FILE_PREFIX = TRACK_FILE_PREFIX + "_#";
    private static final String CHUNK_HEADERS_FILE_NAME = TRACK_FILE_PREFIX + "_chunk_headers.json";
    private static final String EVENTS_FILE_NAME = EVENTS_FILE_PREFIX + "%d.json";
    private static final int MAX_BYTES_PER_CHUNK = 1000000; //1MB

    private final static Type EVENTS_FILE_TYPE = new TypeToken<Map<String, List<Event>>>() {
    }.getType();

    private IStorage mFileStorageManager;
    Map<String, EventsChunk> mEventsChunks;

    public TrackStorageManager(IStorage storage) {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        mFileStorageManager = storage;
        mEventsChunks = Collections.synchronizedMap(new HashMap<>());
        loadEventsFromDisk();
    }

    public boolean isEmptyCache(){
        return mEventsChunks.isEmpty();
    }

    synchronized public void deleteCachedEvents(String chunkId){
        mEventsChunks.remove(chunkId);
    }

    synchronized public void saveEvents(EventsChunk chunk){
        if(chunk == null || chunk.getEvents().isEmpty()) {
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

    private void loadEventsFromDisk() {
        if(mFileStorageManager.exists(CHUNK_HEADERS_FILE_NAME)) {
            loadEventsFromMultipleFiles();
        } else {
            loadEventsFromOnFile();
        }
    }

    private void loadEventsFromOnFile() {
        // Legacy file
        try {
            String storedTracks = mFileStorageManager.read(LEGACY_EVENTS_FILE_NAME);
            if(Strings.isNullOrEmpty(storedTracks)) {
                return;
            }

            Type dataType = new TypeToken<Map<String, EventsChunk>>() {
            }.getType();

            Map<String, EventsChunk> chunkTracks = Json.fromJson(storedTracks, dataType);
            mEventsChunks.putAll(chunkTracks);

        } catch (IOException ioe) {
            Logger.e(ioe, "Unable to load tracks from disk: " + ioe.getLocalizedMessage());
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse saved tracks: " + syntaxException.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e(e, "Error loading tracks from legacy file from disk: " + e.getLocalizedMessage());
        } catch (Throwable t) {
            Logger.e(t, "Could not load legacy file: " + t.getLocalizedMessage());
            mFileStorageManager.delete(LEGACY_EVENTS_FILE_NAME);
        }
    }

    private void loadEventsFromMultipleFiles() {

        try {
            String headerContent = mFileStorageManager.read(CHUNK_HEADERS_FILE_NAME);
            if(headerContent != null) {
                List<ChunkHeader> headers = Json.fromJson(headerContent, ChunkHeader.CHUNK_HEADER_TYPE);
                for (ChunkHeader header : headers) {
                    EventsChunk chunk = new EventsChunk(header.getId(), header.getAttempt());
                    mEventsChunks.put(chunk.getId(), chunk);
                }
            }
        } catch (IOException ioe) {
            Logger.e(ioe, "Unable to track chunks headers information from disk: " + ioe.getLocalizedMessage());
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse saved track chunks headers: " + syntaxException.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e(e, "Error loading tracks headers from disk: " + e.getLocalizedMessage());
        }

        List<Map<String, List<Event>>> events = new ArrayList<>();

        List<String> allFileNames = mFileStorageManager.getAllIds(EVENTS_FILE_PREFIX);
        for (String fileName : allFileNames) {
            try {
                String file = mFileStorageManager.read(fileName);
                Map<String, List<Event>> eventsFile = Json.fromJson(file, EVENTS_FILE_TYPE);
                for(Map.Entry<String, List<Event>> eventsChunk : eventsFile.entrySet()) {
                    String chunkId = eventsChunk.getKey();
                    EventsChunk chunk = mEventsChunks.get(chunkId);
                    if(chunk == null) {
                        chunk = new EventsChunk(chunkId, 0);
                    }
                    chunk.addEvents(eventsChunk.getValue());
                }
            } catch (IOException ioe) {
                Logger.e(ioe, "Unable to track event file from disk: " + ioe.getLocalizedMessage());
            } catch (JsonSyntaxException syntaxException) {
                Logger.e(syntaxException, "Unable to parse saved track event: " + syntaxException.getLocalizedMessage());
            } catch (Exception e) {
                Logger.e(e, "Error loading tracks events from disk: " + e.getLocalizedMessage());
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private void saveToDisk() {
        List<ChunkHeader> headers = getChunkHeaders(mEventsChunks);

        try {
            String json = Json.toJson(headers);
            mFileStorageManager.write(CHUNK_HEADERS_FILE_NAME, json);
        } catch (IOException ioe) {
            Logger.e(ioe, "Could not save tracks headers");
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse tracks to save");
        } catch (Exception e) {
            Logger.e(e, "Error saving tracks events to disk: " + e.getLocalizedMessage());
        } catch (Throwable t) {
            Logger.e(t, "Error saving tracks events to disk: " + t.getLocalizedMessage());
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
            } catch (Throwable t) {
                Logger.e(t, "Could not save: " + t.getLocalizedMessage());
            }
        }
    }

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
                if(bytesCount + event.getSizeInBytes() > MAX_BYTES_PER_CHUNK) {
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

}
