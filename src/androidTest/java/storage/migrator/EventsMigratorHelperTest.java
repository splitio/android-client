package storage.migrator;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import helper.FileHelper;
import io.split.android.client.dtos.Event;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.migrator.EventsMigratorHelperImpl;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.storage.legacy.TrackStorageManager;
import io.split.android.client.track.EventsChunk;
import io.split.android.client.track.ITrackStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.TimeUtils;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class EventsMigratorHelperTest {
    FileHelper mFileHelper = new FileHelper();
    EventsMigratorHelperImpl mMigrator;
    TrackStorageManager mLegacyEventsStorage;
    TimeUtils mTimeUtils = new TimeUtils();

    @Before
    public void setup() {
        File cacheFolder = InstrumentationRegistry.getInstrumentation().getContext().getCacheDir();
        File rootFolder = mFileHelper.emptyAndGetTestFolder(cacheFolder, "events_folder_test");

        ITrackStorage fileStorage = new FileStorage.TracksFileStorage(rootFolder, "events");
        mLegacyEventsStorage = new TrackStorageManager(fileStorage);
        mMigrator = new EventsMigratorHelperImpl(mLegacyEventsStorage);
    }

    @Test
    public void basicMigration() {

        List<EventsChunk> chunks = createEventChunks(0, 4);
        for(EventsChunk chunk : chunks) {
            mLegacyEventsStorage.saveEvents(chunk);
        }

        List<EventEntity> entities = mMigrator.loadLegacyEventsAsEntities();
        Map<String, EventEntity> entityMap = eventsEntityMap(entities);

        EventEntity entity1 = entityMap.get("event_1");
        EventEntity entity2 = entityMap.get("event_25");
        EventEntity entity3 = entityMap.get("event_50");

        Event event1 = Json.fromJson(entity1.getBody(), Event.class);
        Event event2 = Json.fromJson(entity2.getBody(), Event.class);
        Event event3 = Json.fromJson(entity3.getBody(), Event.class);

        Assert.assertEquals(50, entities.size());

        Assert.assertNotNull(entity1);
        Assert.assertNotNull(entity2);
        Assert.assertNotNull(entity3);

        Assert.assertEquals("event_1", event1.eventTypeId);
        Assert.assertEquals("custom", event1.trafficTypeName);
        Assert.assertEquals("the_key", event1.key);
        Assert.assertTrue(entity1.getCreatedAt() > 0);


        Assert.assertEquals("event_25", event2.eventTypeId);
        Assert.assertEquals("custom", event2.trafficTypeName);
        Assert.assertEquals("the_key", event2.key);
        Assert.assertTrue(entity2.getCreatedAt() > 0);

        Assert.assertEquals("event_50", event3.eventTypeId);
        Assert.assertEquals("custom", event3.trafficTypeName);
        Assert.assertEquals("the_key", event3.key);
        Assert.assertTrue(entity3.getCreatedAt() > 0);

    }

    @Test
    public void emptyMigration() {
        List<EventEntity> entities = mMigrator.loadLegacyEventsAsEntities();
        Assert.assertEquals(0, entities.size());
    }

    private Map<String, EventEntity> eventsEntityMap(List<EventEntity> entities) {
        Map<String, EventEntity> entityMap = new HashMap<>();
        for (EventEntity entity : entities) {
            Event event = Json.fromJson(entity.getBody(), Event.class);
            entityMap.put(event.eventTypeId, entity);
        }
        return entityMap;
    }

    private List<EventsChunk> createEventChunks(int from, int to) {
        List<EventsChunk> chunks = new ArrayList<>();
        for(int i=from; i<=to; i++) {
            int eFrom = i * 10 + 1;
            chunks.add(new EventsChunk(createEvents(eFrom, eFrom + 9)));
        }
        return chunks;
    }

    private List<Event> createEvents(int from, int to) {
        List<Event> events = new ArrayList<>();
        for(int i=from; i<=to; i++) {
            Event split = newEvent("event_" + i);
            events.add(split);
        }
        return events;
    }

    private Event newEvent(String type) {
        Event event = new Event();
        event.trafficTypeName = "custom";
        event.eventTypeId = type;
        event.value = 1.0;
        event.key = "the_key";
        event.timestamp = mTimeUtils.timeInSeconds();
        return event;
    }
}
