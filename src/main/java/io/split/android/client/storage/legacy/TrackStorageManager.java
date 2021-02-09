package io.split.android.client.storage.legacy;


import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.dtos.Event;
import io.split.android.client.storage.legacy.FileStorageHelper;
import io.split.android.client.track.EventsChunk;
import io.split.android.client.track.ITrackStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.MemoryUtils;
import io.split.android.client.utils.MemoryUtilsImpl;

@Deprecated
public class TrackStorageManager {

    public static final String LEGACY_EVENTS_FILE_NAME = "SPLITIO.events.json";
    private static final String TRACK_FILE_PREFIX = "SPLITIO.events";
    private static final String EVENTS_FILE_PREFIX = TRACK_FILE_PREFIX + "_#";
    public static final String CHUNK_HEADERS_FILE_NAME = TRACK_FILE_PREFIX + "_chunk_headers.json";
    private static final Type LEGACY_FILE_TYPE = new TypeToken<Map<String, EventsChunk>>() {
    }.getType();

    private FileStorageHelper mFileStorageHelper;

    private final static Type EVENTS_FILE_TYPE = new TypeToken<Map<String, List<Event>>>() {
    }.getType();

    private ITrackStorage mFileStorageManager;
    Map<String, EventsChunk> mEventsChunks;

    public TrackStorageManager(ITrackStorage storage) {
        this(storage, new MemoryUtilsImpl());
    }

    public TrackStorageManager(ITrackStorage storage, MemoryUtils memoryUtils) {
        mFileStorageManager = storage;
        mFileStorageHelper = new FileStorageHelper(memoryUtils);
        mEventsChunks = new ConcurrentHashMap<>();
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
            return;
        }

        mEventsChunks.put(chunk.getId(), chunk);
    }

    public List<EventsChunk> getEventsChunks() {
        return new ArrayList<>(mEventsChunks.values());
    }

    public void saveToDisk() {
        mFileStorageManager.write(mEventsChunks);
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
        Map<String, EventsChunk> loaded = mFileStorageManager.read();
        if (loaded != null) {
            mEventsChunks.putAll(loaded);
        }
    }

    private void loadEventsFromLegacyFile() {
        try {
            String storedTracks = mFileStorageHelper.checkMemoryAndReadFile(LEGACY_EVENTS_FILE_NAME, mFileStorageManager);
            if (!Strings.isNullOrEmpty(storedTracks)) {
                Map<String, EventsChunk> chunkTracks = Json.fromJson(storedTracks, LEGACY_FILE_TYPE);
                mEventsChunks.putAll(chunkTracks);
            }
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse saved tracks: " + syntaxException.getLocalizedMessage());
        }
    }

    private void loadEventsFromChunkFiles() {
        createChunksFromHeaders(mFileStorageHelper.readAndParseChunkHeadersFile(CHUNK_HEADERS_FILE_NAME, mFileStorageManager));
        createEventsFromChunkFiles();
        removeChunksWithoutEvents();
    }

    private void createChunksFromHeaders(List<ChunkHeader> headers) {
        if(headers != null) {
            for (ChunkHeader header : headers) {
                EventsChunk chunk = new EventsChunk(header.getId(), header.getAttempt());
                mEventsChunks.put(chunk.getId(), chunk);
            }
        }
    }

    private void createEventsFromChunkFiles() {
        List<String> allFileNames = mFileStorageManager.getAllIds(EVENTS_FILE_PREFIX);
        for (String fileName : allFileNames) {
            String fileContent = mFileStorageHelper.checkMemoryAndReadFile(fileName, mFileStorageManager);
            if(fileContent != null) {
                parseEvents(fileContent);
            }
        }
    }

    private Map<String, List<Event>> parseEventsChunkFileContent(String json) {
        Map<String, List<Event>> impressionsFile = null;
        try {
            impressionsFile = Json.fromJson(json, EVENTS_FILE_TYPE);
        } catch (JsonSyntaxException e) {
            Logger.e("Unable to parse saved track event: " + e.getLocalizedMessage());
        }
        return impressionsFile;
    }

    private void parseEvents(String json) {
        Map<String, List<Event>> eventsFile = parseEventsChunkFileContent(json);
        if(eventsFile == null) {
            return;
        }

        for (Map.Entry<String, List<Event>> eventsChunk : eventsFile.entrySet()) {
            String chunkId = eventsChunk.getKey();
            EventsChunk chunk = mEventsChunks.get(chunkId);
            if (chunk == null) {
                chunk = new EventsChunk(chunkId, 0);
            }
            chunk.addEvents(eventsChunk.getValue());
        }
    }

    private void removeChunksWithoutEvents() {
        List<String> chunkIds = new ArrayList(mEventsChunks.keySet());
        for(String chunkId : chunkIds) {
            EventsChunk chunk = mEventsChunks.get(chunkId);
            if(chunk != null && chunk.getEvents() != null && chunk.getEvents().size() == 0) {
                mEventsChunks.remove(chunkId);
            }
        }
    }

    private void deleteOldChunksFiles() {
        List<String> oldChunkFiles = mFileStorageManager.getAllIds(EVENTS_FILE_PREFIX);
        for(String fileName : oldChunkFiles) {
            mFileStorageManager.delete(fileName);
        }
        mFileStorageManager.delete(CHUNK_HEADERS_FILE_NAME);
    }

    public void deleteAllFiles() {
        List<String> filesToDelete = mFileStorageManager.getAllIds(TRACK_FILE_PREFIX);
        mFileStorageManager.delete(filesToDelete);
    }
}
