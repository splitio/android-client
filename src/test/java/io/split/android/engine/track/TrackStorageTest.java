package io.split.android.engine.track;

import com.google.gson.reflect.TypeToken;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.dtos.Event;
import io.split.android.client.storage.IStorage;
import io.split.android.client.storage.MemoryStorage;
import io.split.android.client.track.EventsChunk;
import io.split.android.client.track.ITrackStorage;
import io.split.android.client.track.TrackStorageManager;
import io.split.android.client.track.TracksFileStorage;
import io.split.android.client.utils.Json;
import io.split.android.fake.MemoryUtilsNoMemoryStub;
import io.split.android.fake.TrackFileStorageStub;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TrackStorageTest {

    TrackStorageManager mTrackStorage = null;
    Set<String> mInitialChunkIds = null;

    Type chunkHeaderType = new TypeToken<List<ChunkHeader>>() {
    }.getType();
    Type eventsFileType = new TypeToken<Map<String, List<Event>>>() {
    }.getType();
    final String CHUNK_HEADERS_FILE_NAME = "SPLITIO.events_chunk_headers.json";
    final String EVENTS_FILE_NAME = "SPLITIO.events_#%d.json";
    final int MAX_FILE_SIZE = 1000000;
    ITrackStorage mStorage;


    @Before
    public void setup(){
        File rootFolder = new File("./build");
        File folder = new File(rootFolder, "test_folder");
        if(folder.exists()) {
            for(File file : folder.listFiles()){
                file.delete();
            }
            folder.delete();
        }
        mStorage = new TracksFileStorage(rootFolder, "test_folder");
        mTrackStorage = new TrackStorageManager(mStorage);

    }

    @Test
    public void saveEvents(){

        List<Event> events  = new ArrayList<>();
        Event event = new Event();
        event.eventTypeId = "type-test";
        event.key = "key-test";
        events.add(event);

        EventsChunk chunk = new EventsChunk(events);
        String chunkIdAdded = chunk.getId();
        mTrackStorage.saveEvents(chunk);

        List<EventsChunk> chunks = mTrackStorage.getEventsChunks();
        int chunkAddedIndex = getIndexForChunk(chunkIdAdded, chunks);
        Assert.assertEquals(1, chunks.size());
        Assert.assertNotEquals(-1, chunkAddedIndex);

        List<Event> eventsAdded = chunks.get(chunkAddedIndex).getEvents();
        Assert.assertEquals(1, eventsAdded.size());
        Assert.assertEquals("type-test", eventsAdded.get(0).eventTypeId);
    }

    @Test
    public void deleteEventsChunk() {
        List<Event> events  = new ArrayList<>();
        Event event = new Event();
        event.eventTypeId = "type-test";
        event.key = "key-test";
        events.add(event);

        EventsChunk chunk = new EventsChunk(events);
        String chunkIdAdded = chunk.getId();
        mTrackStorage.saveEvents(chunk);

        List<EventsChunk> chunks = mTrackStorage.getEventsChunks();
        String chunkIdToRemove = chunks.get(0).getId();
        mTrackStorage.deleteCachedEvents(chunkIdToRemove);
        chunks = mTrackStorage.getEventsChunks();
        Assert.assertEquals(0, chunks.size());
        Assert.assertEquals(-1, getIndexForChunk(chunkIdToRemove, chunks));
    }

    @Test
    public void testSaveAndLoadChunkFiles() throws IOException {


        final int chunkCount = 10;


        int[][] chunksData = {
                {35, 4, 86, 40, 200, 120, 20, 420, 8, 911},
                {1100, 1305, 4506, 7530, 3209, 5230, 6500, 6880, 4100, 23000},
        };

        TrackStorageManager savingManager = new TrackStorageManager(mStorage);

        for(int i = 0; i < chunkCount; i++) {
            int eventCount = chunksData[0][i];
            int eventSize = chunksData[1][i];
            List<Event> events  = new ArrayList<>();
            for (int j = 0; j < eventCount; j++) {
                Event event = new Event();
                event.eventTypeId = String.format("type-%d-%d", i, j);
                event.key = String.format("key-%d-%d", i, j);
                event.setSizeInBytes(eventSize);
                events.add(event);
            }
            EventsChunk chunk = new EventsChunk(events);
            chunk.addAtempt();

            if(i % 3 == 2) {
                chunk.addAtempt();
            } else if(i % 3 == 1) {
                chunk.addAtempt();
            }
            savingManager.saveEvents(chunk);
        }

        List<EventsChunk> savedChunks = savingManager.getEventsChunks();

        savingManager.close(); // Close saves to disk

        TrackStorageManager loadingManager = new TrackStorageManager(mStorage);
        List<EventsChunk> loadedChunks = loadingManager.getEventsChunks();

        Assert.assertEquals(10, loadedChunks.size());
        Assert.assertEquals(savedChunks.size(), loadedChunks.size());
        for(EventsChunk savedChunk : savedChunks) {
            EventsChunk loadedChunk = loadedChunks.get(getIndexForChunk(savedChunk.getId(), loadedChunks));
            Assert.assertEquals(savedChunk.getAttempt(), loadedChunk.getAttempt());
            Assert.assertEquals(savedChunk.getEvents().size(), loadedChunk.getEvents().size());
        }
    }

    @Test
    public void testLoadLegacyFromLegacyFile() throws IOException {


        final int chunkCount = 10;
        ITrackStorage memStorage = new TrackFileStorageStub();
        int[][] chunksData = {
                {35, 4, 86, 40, 200, 120, 20, 420, 8, 911},
                {1100, 1305, 4506, 7530, 3209, 5230, 6500, 6880, 4100, 23000},
        };

        populateStorageWithLegacyFile(chunksData, memStorage);

        TrackStorageManager manager = new TrackStorageManager(memStorage);
        List<EventsChunk> loadedChunks = manager.getEventsChunks();

        Assert.assertEquals(10, loadedChunks.size());
        for(int i = 0; i< 10; i++) {
            EventsChunk loadedChunk = loadedChunks.get(getIndexForChunk("id_" + i, loadedChunks));
            Assert.assertEquals((i % 3), loadedChunk.getAttempt());
            Assert.assertEquals(chunksData[0][i], loadedChunk.getEvents().size());
        }
        Assert.assertEquals(0, memStorage.getAllIds().length);
    }

    @Test
    public void testNoAvailableMemoryLoadingLegacyFile() throws IOException {

        final int chunkCount = 10;
        ITrackStorage memStorage = new TrackFileStorageStub();
        int[][] chunksData = {
                {35, 4, 86, 40, 200, 120, 20, 420, 8, 911},
                {1100, 1305, 4506, 7530, 3209, 5230, 6500, 6880, 4100, 23000},
        };

        populateStorageWithLegacyFile(chunksData, memStorage);

        TrackStorageManager manager = new TrackStorageManager(memStorage, new MemoryUtilsNoMemoryStub());
        List<EventsChunk> loadedChunks = manager.getEventsChunks();

        Assert.assertEquals(0, loadedChunks.size());
    }

    @Test
    public void testLoadFromLegacyChunkFiles() throws IOException {

        final int chunkCount = 10;
        ITrackStorage memStorage = new TrackFileStorageStub();
        int[][] chunksData = {
                {35, 4, 86, 40, 200, 120, 20, 420, 8, 911},
                {1100, 1305, 4506, 7530, 3209, 5230, 6500, 6880, 4100, 23000},
        };

        populateStorageWithLegacyChunkFiles(chunksData, memStorage);

        TrackStorageManager manager = new TrackStorageManager(memStorage);
        List<EventsChunk> loadedChunks = manager.getEventsChunks();

        Assert.assertEquals(10, loadedChunks.size());
        for(int i = 0; i< 10; i++) {
            int index = getIndexForChunk("id_" + i, loadedChunks);
            EventsChunk loadedChunk = loadedChunks.get(index);
            Assert.assertEquals(1, loadedChunk.getAttempt());
            Assert.assertEquals(chunksData[0][i], loadedChunk.getEvents().size());
        }
        Assert.assertEquals(0, memStorage.getAllIds().length);
    }

    @Test
    public void testMissingEventsFile() throws IOException {
        ITrackStorage memStorage = new TrackFileStorageStub();
        List<ChunkHeader> headers = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            ChunkHeader c = new ChunkHeader("c" + i, 0);
            headers.add(c);
        }
        String json = Json.toJson(headers);
        memStorage.write(CHUNK_HEADERS_FILE_NAME, json);

        TrackStorageManager manager = new TrackStorageManager(memStorage);

        Assert.assertNotNull(manager);

    }

    @Test
    public void testUnavailableMemoryLoadingFileChunks() throws IOException {

        final int chunkCount = 10;
        ITrackStorage memStorage = new TrackFileStorageStub();
        int[][] chunksData = {
                {35, 4, 86, 40, 200, 120, 20, 420, 8, 911},
                {1100, 1305, 4506, 7530, 3209, 5230, 6500, 6880, 4100, 23000},
        };

        populateStorageWithLegacyChunkFiles(chunksData, memStorage);

        TrackStorageManager manager = new TrackStorageManager(memStorage, new MemoryUtilsNoMemoryStub());
        List<EventsChunk> loadedChunks = manager.getEventsChunks();

        Assert.assertEquals(0, loadedChunks.size());
        Assert.assertEquals(0, memStorage.getAllIds().length);
    }

    @Test
    public void loadEmptyJsonLFile() throws IOException {
        mStorage.write("SPLITIO.events_chunk_id_1.jsonl","");
        TrackStorageManager manager = new TrackStorageManager(mStorage);
        List<EventsChunk> chunk = manager.getEventsChunks();

        Assert.assertNotNull(chunk);
        Assert.assertEquals(0, chunk.size());

    }

    // Helpers
    private void populateStorageWithLegacyFile(int[][] chunksData, ITrackStorage storage) throws IOException {

        final String LEGACY_EVENTS_FILE_NAME = "SPLITIO.events.json";
        Map<String, EventsChunk> chunks = new HashMap<>();
        for(int i = 0; i < 10; i++) {
            int eventCount = chunksData[0][i];
            int eventSize = chunksData[1][i];
            List<Event> events  = new ArrayList<>();
            for (int j = 0; j < eventCount; j++) {
                Event event = new Event();
                event.eventTypeId = String.format("type-%d-%d", i, j);
                event.key = String.format("key-%d-%d", i, j);
                event.setSizeInBytes(eventSize);
                events.add(event);
            }
            EventsChunk chunk = new EventsChunk("id_" + i, (i % 3));
            chunk.addEvents(events);
            chunks.put(chunk.getId(), chunk);
        }

        String jsonChunks = Json.toJson(chunks);
        storage.write(LEGACY_EVENTS_FILE_NAME, jsonChunks);
    }

    private void populateStorageWithLegacyChunkFiles(int[][] chunksData, ITrackStorage storage) throws IOException{
        final String CHUNK_HEADERS_FILE = "SPLITIO.events_chunk_headers.json";
        final String EVENTS_FILE_PREFIX = "SPLITIO.events_#";

        List<ChunkHeader> headers = new ArrayList<>();
        for(int i = 0; i < 10; i++) {
            int eventCount = chunksData[0][i];
            int eventSize = chunksData[1][i];
            Map<String, List<Event>> chunkEvents = new HashMap<>();
            List<Event> events  = new ArrayList<>();
            for (int j = 0; j < eventCount; j++) {
                Event event = new Event();
                event.eventTypeId = String.format("type-%d-%d", i, j);
                event.key = String.format("key-%d-%d", i, j);
                event.setSizeInBytes(eventSize);
                events.add(event);
            }
            String chunkId = "id_" + i;
            ChunkHeader header = new ChunkHeader(chunkId, 1);
            headers.add(header);
            chunkEvents.put(chunkId, events);
            String json = Json.toJson(chunkEvents);
            storage.write(EVENTS_FILE_PREFIX + i + ".json", json);
        }

        String jsonChunks = Json.toJson(headers);
        storage.write(CHUNK_HEADERS_FILE, jsonChunks);
    }


    private int populateStorageManager(TrackStorageManager manager) {
        int totalSize = 0;
        final int chunkCount = 10;
        IStorage memStorage = new MemoryStorage();
        int[][] chunksData = {
                {35, 4, 86, 40, 200, 120, 20, 420, 8, 911},
                {1100, 1305, 4506, 7530, 3209, 5230, 6500, 6880, 4100, 23000},
        };

        for(int i = 0; i < chunkCount; i++) {
            int eventCount = chunksData[0][i];
            int eventSize = chunksData[1][i];
            List<Event> events  = new ArrayList<>();
            for (int j = 0; j < eventCount; j++) {
                Event event = new Event();
                event.eventTypeId = String.format("type-%d-%d", i, j);
                event.key = String.format("key-%d-%d", i, j);
                event.setSizeInBytes(eventSize);
                events.add(event);
                totalSize += eventSize;
            }
            EventsChunk chunk = new EventsChunk(events);
            chunk.addAtempt();
            manager.saveEvents(chunk);
        }
        return totalSize;
    }

    private int getIndexForChunk(String chunkId, List<EventsChunk> chunks) {

        int index = -1;
        int i = 0;
        boolean found = false;

        while(!found && chunks.size() > i){
            String id = chunks.get(i).getId();
            if(chunkId.equals(id)){
                found = true;
                index = i;
            }
            i++;
        }
        return index;
    }

    private int sizeInBytes(Map<String, List<Event>> eventsPerChunk) {
        int sum = 0;
        for (List<Event> events : eventsPerChunk.values()) {
            sum += sizeInBytes(events);
        }
        return sum;
    }

    private int sizeInBytes(List<Event> events) {
        int sum = 0;
        for (Event event : events) {
            sum += event.getSizeInBytes();
        }
        return sum;
    }
}
