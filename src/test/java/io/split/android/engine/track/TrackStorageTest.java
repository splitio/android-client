package io.split.android.engine.track;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.impressions.StoredImpressions;
import io.split.android.client.storage.IStorage;
import io.split.android.client.storage.MemoryStorage;
import io.split.android.client.track.EventsChunk;
import io.split.android.client.track.TrackStorageManager;
import io.split.android.client.utils.Json;

public class TrackStorageTest {

    TrackStorageManager mTrackStorage = null;
    Set<String> mInitialChunkIds = null;

    @Before
    public void setupUp(){

        final String FILE_NAME = "SPLITIO.events.json";

        Map<String, EventsChunk> eventsChunks = new HashMap<>();
        IStorage memStorage = new MemoryStorage();
        mInitialChunkIds = new HashSet<>();
        final int CHUNK_COUNT = 4;
        for(int i = 0; i < CHUNK_COUNT; i++) {
            List<Event> events  = new ArrayList<>();
            for(int j = 0; j < 4; j++) {
                Event event = new Event();
                event.eventTypeId = String.format("type-%d-%d", i, j);
                event.key = String.format("key-%d-%d", i, j);
                events.add(event);
            }

            EventsChunk chunk = new EventsChunk(events);
            mInitialChunkIds.add(chunk.getId());
            eventsChunks.put(chunk.getId(), chunk);
        }
        try {
            String allChunks = Json.toJson(eventsChunks);
            memStorage.write(FILE_NAME, allChunks);
        } catch (IOException e) {
        }
        mTrackStorage = new TrackStorageManager(memStorage);
    }

    @Test
    public void getEventsChunks() {
        List<EventsChunk> chunks = mTrackStorage.getEventsChunks();
        List<String> chunkIds = new ArrayList<>(mInitialChunkIds);
        Assert.assertEquals(4, chunks.size());
        Assert.assertNotEquals(-1, getIndexForChunk(chunkIds.get(0), chunks));
        Assert.assertNotEquals(-1, getIndexForChunk(chunkIds.get(1), chunks));
        Assert.assertNotEquals(-1, getIndexForChunk(chunkIds.get(2), chunks));
        Assert.assertNotEquals(-1, getIndexForChunk(chunkIds.get(3), chunks));

        Assert.assertEquals(4, chunks.get(0).getEvents().size());
        Assert.assertEquals(4, chunks.get(3).getEvents().size());
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
        Assert.assertEquals(5, chunks.size());
        Assert.assertNotEquals(-1, chunkAddedIndex);

        List<Event> eventsAdded = chunks.get(chunkAddedIndex).getEvents();
        Assert.assertEquals(1, eventsAdded.size());
        Assert.assertEquals("type-test", eventsAdded.get(0).eventTypeId);
    }

    @Test
    public void deleteEventsChunk() {
        List<EventsChunk> chunks = mTrackStorage.getEventsChunks();
        String chunkIdToRemove = chunks.get(0).getId();
        mTrackStorage.deleteCachedEvents(chunkIdToRemove);
        chunks = mTrackStorage.getEventsChunks();
        Assert.assertEquals(3, chunks.size());
        Assert.assertEquals(-1, getIndexForChunk(chunkIdToRemove, chunks));
    }

    // Helpers
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
}
