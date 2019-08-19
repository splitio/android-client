package io.split.android.client.track;


import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;

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
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.MemoryUtils;
import io.split.android.client.utils.MemoryUtilsImpl;

public class TrackStorageManager implements LifecycleObserver {

    private static final String LEGACY_EVENTS_FILE_NAME = "SPLITIO.events.json";
    private static final String TRACK_FILE_PREFIX = "SPLITIO.events";
    private static final String EVENTS_FILE_PREFIX = TRACK_FILE_PREFIX + "_#";
    private static final String CHUNK_HEADERS_FILE_NAME = TRACK_FILE_PREFIX + "_chunk_headers.json";
    private static final int MAX_BYTES_PER_CHUNK = 1000000; //1MB
    private static final int MEMORY_ALLOCATION_TIMES = 2;
    private MemoryUtils mMemoryUtils;

    private final static Type EVENTS_FILE_TYPE = new TypeToken<Map<String, List<Event>>>() {
    }.getType();

    private ITrackStorage mFileStorageManager;
    Map<String, EventsChunk> mEventsChunks;

    public TrackStorageManager(ITrackStorage storage) {
        this(storage, new MemoryUtilsImpl());
    }

    public TrackStorageManager(ITrackStorage storage, MemoryUtils memoryUtils) {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        mMemoryUtils = memoryUtils;
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

    synchronized public List<EventsChunk> takeAll() {
        List<EventsChunk> values = new ArrayList<>(mEventsChunks.values());
        mEventsChunks.clear();
        return values;
    }

    synchronized public void saveEvents(EventsChunk chunk){
        if(chunk == null || chunk.getEvents() != null && chunk.getEvents().isEmpty()) {
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
        if(mFileStorageManager.exists(LEGACY_EVENTS_FILE_NAME)) {
            loadEventsFromLegacyFile();
            mFileStorageManager.delete(LEGACY_EVENTS_FILE_NAME);
        } else if(mFileStorageManager.exists(CHUNK_HEADERS_FILE_NAME)) {
            loadEventsFromChunkFiles();
            deleteOldChunksFiles();
        } else {
            loadEventsFilesByLine();
        }
    }

    private void loadEventsFilesByLine() {
        try {
            Map<String, EventsChunk> loaded = mFileStorageManager.read();
            if (loaded != null) {
                mEventsChunks.putAll(loaded);
            }
        } catch (IOException ioe) {
            Logger.e(ioe, "Unable to track event file from disk: " + ioe.getLocalizedMessage());
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse saved track event: " + syntaxException.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e(e, "Error loading tracks events from disk: " + e.getLocalizedMessage());
        }
    }

    private void loadEventsFromChunkFiles() {

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
                long fileSize = mFileStorageManager.fileSize(fileName);
                if(mMemoryUtils.isMemoryAvailableToAllocate(fileSize, MEMORY_ALLOCATION_TIMES)) {
                    String file = mFileStorageManager.read(fileName);
                    Map<String, List<Event>> eventsFile = Json.fromJson(file, EVENTS_FILE_TYPE);
                    for (Map.Entry<String, List<Event>> eventsChunk : eventsFile.entrySet()) {
                        String chunkId = eventsChunk.getKey();
                        EventsChunk chunk = mEventsChunks.get(chunkId);
                        if (chunk == null) {
                            chunk = new EventsChunk(chunkId, 0);
                        }
                        chunk.addEvents(eventsChunk.getValue());
                    }
                } else {
                    Logger.w("Unable to parse track file " + fileName + ". Memory not available");
                }

            } catch (IOException ioe) {
                Logger.e(ioe, "Unable to track event file from disk: " + ioe.getLocalizedMessage());
            } catch (JsonSyntaxException syntaxException) {
                Logger.e(syntaxException, "Unable to parse saved track event: " + syntaxException.getLocalizedMessage());
            } catch (Exception e) {
                Logger.e(e, "Error loading tracks events from disk: " + e.getLocalizedMessage());
            }
        }
        List<String> chunkIds = new ArrayList(mEventsChunks.keySet());
        for(String chunkId : chunkIds) {
            EventsChunk chunk = mEventsChunks.get(chunkId);
            if(chunk != null && chunk.getEvents() != null && chunk.getEvents().size() == 0) {
                mEventsChunks.remove(chunkId);
            }
        }
    }

    private void loadEventsFromLegacyFile() {
        // Legacy file
        try {
            long fileSize = mFileStorageManager.fileSize(LEGACY_EVENTS_FILE_NAME);
            if(mMemoryUtils.isMemoryAvailableToAllocate(fileSize, MEMORY_ALLOCATION_TIMES)) {
                String storedTracks = mFileStorageManager.read(LEGACY_EVENTS_FILE_NAME);
                if (Strings.isNullOrEmpty(storedTracks)) {
                    return;

                }
                Type dataType = new TypeToken<Map<String, EventsChunk>>() {
                }.getType();

                Map<String, EventsChunk> chunkTracks = Json.fromJson(storedTracks, dataType);
                mEventsChunks.putAll(chunkTracks);
            } else {
                Logger.w("Unable to load track file " + LEGACY_EVENTS_FILE_NAME + ". Memory not available");
            }

        } catch (IOException ioe) {
            Logger.e(ioe, "Unable to load tracks from disk: " + ioe.getLocalizedMessage());
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse saved tracks: " + syntaxException.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e(e, "Error loading tracks from legacy file from disk: " + e.getLocalizedMessage());
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void saveToDisk() {
        try {
            mFileStorageManager.write(mEventsChunks);
        } catch (IOException ioe) {
            Logger.e(ioe, "Unable to save tracks to disk: " + ioe.getLocalizedMessage());
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse tracks to save: " + syntaxException.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e(e, "Error saving tracks from legacy file from disk: " + e.getLocalizedMessage());
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

    private void deleteOldChunksFiles() {
        List<String> oldChunkFiles = mFileStorageManager.getAllIds(EVENTS_FILE_PREFIX);
        for(String fileName : oldChunkFiles) {
            mFileStorageManager.delete(fileName);
        }
        mFileStorageManager.delete(CHUNK_HEADERS_FILE_NAME);
    }
}
