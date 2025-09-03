package io.split.android.integration.storage;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.events.SqLitePersistentEventsStorage;
import io.split.android.client.utils.Json;
import io.split.android.integration.DatabaseHelper;

@RunWith(RobolectricTestRunner.class)
public class PersistentEventStorageTest {

    final static long EXPIRATION_PERIOD = 3600 * 24;
    SplitRoomDatabase mRoomDb;
    Context mContext;
    PersistentEventsStorage mPersistentEventsStorage;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();
        generateEvents(1, 10, StorageRecordStatus.ACTIVE, false);
        generateEvents(101, 110, StorageRecordStatus.DELETED, false);
        generateEvents(301, 310, StorageRecordStatus.ACTIVE, true);

        mPersistentEventsStorage = new SqLitePersistentEventsStorage(mRoomDb, EXPIRATION_PERIOD,
                SplitCipherFactory.create("abcdefghijklmnopqrstuvxyz", false));
    }

    @Test
    public void create() {
        List<Event> events = createEvents(201, 210, StorageRecordStatus.ACTIVE);
        for (Event event : events) {
            mPersistentEventsStorage.push(event);
        }
        List<EventEntity> first10ActiveLoadedEvents = mRoomDb.eventDao().getBy(0,
                StorageRecordStatus.ACTIVE, 10);
        List<EventEntity> allActiveLoadedEvents = mRoomDb.eventDao().getBy(0, StorageRecordStatus.ACTIVE, 100);
        List<EventEntity> first5DeletedLoadedEvents = mRoomDb.eventDao().getBy(0, StorageRecordStatus.DELETED, 5);
        List<EventEntity> allDeletedLoadedEvents = mRoomDb.eventDao().getBy(0, StorageRecordStatus.DELETED, 100);
        List<EventEntity> noEvents = mRoomDb.eventDao().getBy(0, StorageRecordStatus.DELETED, 0);

        boolean first10ActiveStatus = checkStatus(first10ActiveLoadedEvents, StorageRecordStatus.ACTIVE);
        boolean allActiveStatus = checkStatus(allActiveLoadedEvents, StorageRecordStatus.ACTIVE);
        boolean first5DeletedStatus = checkStatus(first5DeletedLoadedEvents, StorageRecordStatus.DELETED);
        boolean allDeletedStatus = checkStatus(allDeletedLoadedEvents, StorageRecordStatus.DELETED);

        Event firstActiveEvent = Json.fromJson(first10ActiveLoadedEvents.get(0).getBody(), Event.class);
        Event lastActiveEvent = Json.fromJson(first10ActiveLoadedEvents.get(9).getBody(), Event.class);

        Event firstAllActiveEvent = Json.fromJson(allActiveLoadedEvents.get(0).getBody(), Event.class);
        Event lastAllActiveEvent = Json.fromJson(allActiveLoadedEvents.get(19).getBody(), Event.class);

        Event first5DeletedEvent = Json.fromJson(first5DeletedLoadedEvents.get(0).getBody(), Event.class);
        Event last5DeletedEvent = Json.fromJson(first5DeletedLoadedEvents.get(4).getBody(), Event.class);

        Event firstAllDeletedEvent = Json.fromJson(allDeletedLoadedEvents.get(0).getBody(), Event.class);
        Event lastAllDeletedEvent = Json.fromJson(allDeletedLoadedEvents.get(9).getBody(), Event.class);

        Assert.assertEquals(10, first10ActiveLoadedEvents.size());
        Assert.assertEquals(30, allActiveLoadedEvents.size());
        Assert.assertEquals(5, first5DeletedLoadedEvents.size());
        Assert.assertEquals(10, allDeletedLoadedEvents.size());
        Assert.assertEquals(0, noEvents.size());

        Assert.assertTrue(first10ActiveStatus);
        Assert.assertTrue(allActiveStatus);
        Assert.assertTrue(first5DeletedStatus);
        Assert.assertTrue(allDeletedStatus);

        Assert.assertEquals("event_301", firstActiveEvent.eventTypeId);
        Assert.assertEquals("event_310", lastActiveEvent.eventTypeId);

        Assert.assertEquals("event_301", firstAllActiveEvent.eventTypeId);
        Assert.assertEquals("event_10", lastAllActiveEvent.eventTypeId);

        Assert.assertEquals("event_101", first5DeletedEvent.eventTypeId);
        Assert.assertEquals("event_105", last5DeletedEvent.eventTypeId);

        Assert.assertEquals("event_101", firstAllDeletedEvent.eventTypeId);
        Assert.assertEquals("event_110", lastAllDeletedEvent.eventTypeId);

    }

    @Test
    public void pop() {

        List<Event> events1 = mPersistentEventsStorage.pop(5);
        List<Event> events2 = mPersistentEventsStorage.pop(100);
        List<EventEntity> activeEvents = mRoomDb.eventDao().getBy(0, StorageRecordStatus.ACTIVE, 100);
        List<EventEntity> deletedEvents = mRoomDb.eventDao().getBy(0, StorageRecordStatus.DELETED, 100);

        Assert.assertEquals(5, events1.size());
        Assert.assertEquals(5, events2.size());
        Assert.assertEquals("event_1", events1.get(0).eventTypeId);
        Assert.assertEquals("event_5", events1.get(4).eventTypeId);
        Assert.assertEquals(10, activeEvents.size());
        Assert.assertEquals(20, deletedEvents.size());
    }

    @Test
    public void popMasive() {
        // To make sure that poping in chunks works as expected
        mRoomDb.clearAllTables();
        List<Event> events = createEvents(1000, 6000, StorageRecordStatus.ACTIVE);
        for(Event event : events) {
            mPersistentEventsStorage.push(event);
        }
        List<Event> Impressions1 = mPersistentEventsStorage.pop(2000);
        List<Event> Impressions2 = mPersistentEventsStorage.pop(2001);
        List<EventEntity> activeImpressions = mRoomDb.eventDao().getBy(0, StorageRecordStatus.ACTIVE, 10000);
        List<EventEntity> deletedImpressions = mRoomDb.eventDao().getBy(0, StorageRecordStatus.DELETED, 10000);

        Assert.assertEquals(2000, Impressions1.size());
        Assert.assertEquals(2001, Impressions2.size());
        Assert.assertEquals(4001, deletedImpressions.size());
        Assert.assertEquals(1000, activeImpressions.size());
    }

    @Test
    public void setActive() {

        List<Event> events = mPersistentEventsStorage.pop(100);
        List<EventEntity> activeEventsBefore = mRoomDb.eventDao().getBy(0, StorageRecordStatus.ACTIVE, 100);
        List<EventEntity> deletedEventsBefore = mRoomDb.eventDao().getBy(0, StorageRecordStatus.DELETED, 100);
        mPersistentEventsStorage.setActive(events);
        List<EventEntity> activeEvents = mRoomDb.eventDao().getBy(0, StorageRecordStatus.ACTIVE, 100);
        List<EventEntity> deletedEventsAfter = mRoomDb.eventDao().getBy(0, StorageRecordStatus.DELETED, 100);

        Assert.assertEquals(10, events.size());
        Assert.assertEquals(10, activeEventsBefore.size());
        Assert.assertEquals(20, deletedEventsBefore.size());
        Assert.assertEquals(20, activeEvents.size());
        Assert.assertEquals(10, deletedEventsAfter.size());
    }

    @Test
    public void setActiveMasive() {

        mRoomDb.clearAllTables();
        List<Event> masiveEvents = createEvents(1, 4000, StorageRecordStatus.ACTIVE);
        for(Event event : masiveEvents) {
            mPersistentEventsStorage.push(event);
        }

        List<Event> events = mPersistentEventsStorage.pop(3001);
        List<EventEntity> activeEventsBefore = mRoomDb.eventDao().getBy(0, StorageRecordStatus.ACTIVE, 10000);
        List<EventEntity> deletedEventsBefore = mRoomDb.eventDao().getBy(0, StorageRecordStatus.DELETED, 10000);
        mPersistentEventsStorage.setActive(events);
        List<EventEntity> activeEvents = mRoomDb.eventDao().getBy(0, StorageRecordStatus.ACTIVE, 10000);
        List<EventEntity> deletedEventsAfter = mRoomDb.eventDao().getBy(0, StorageRecordStatus.DELETED, 10000);

        Assert.assertEquals(3001, events.size());
        Assert.assertEquals(999, activeEventsBefore.size());
        Assert.assertEquals(3001, deletedEventsBefore.size());
        Assert.assertEquals(4000, activeEvents.size());
        Assert.assertEquals(0, deletedEventsAfter.size());
    }

    @Test
    public void masiveDelete() {

        mRoomDb.clearAllTables();
        List<Event> masiveEvents = createEvents(1, 4000, StorageRecordStatus.ACTIVE);
        for(Event event : masiveEvents) {
            mPersistentEventsStorage.push(event);
        }

        List<Event> toDelete =  mPersistentEventsStorage.pop(3000);
        mPersistentEventsStorage.setActive(toDelete);

        mPersistentEventsStorage.delete(toDelete);
        List<EventEntity> activeEvents = mRoomDb.eventDao().getBy(0, StorageRecordStatus.ACTIVE, 10000);
        List<EventEntity> deletedEvents = mRoomDb.eventDao().getBy(0, StorageRecordStatus.DELETED, 10000);

        Assert.assertEquals(1000, activeEvents.size());
        Assert.assertEquals(0, deletedEvents.size());
    }

    @Test
    public void deleteMasiveOutdated() {

        mRoomDb.clearAllTables();
        List<Event> masiveEvents = createEvents(1, 6000, StorageRecordStatus.ACTIVE);
        for(Event event : masiveEvents) {
            mPersistentEventsStorage.push(event);
        }

        List<Event> toDelete =  mPersistentEventsStorage.pop(5000);

        mPersistentEventsStorage.deleteInvalid((System.currentTimeMillis() / 1000) + 2000);
        List<EventEntity> activeEvents = mRoomDb.eventDao().getBy(0, StorageRecordStatus.ACTIVE, 10000);
        List<EventEntity> deletedEvents = mRoomDb.eventDao().getBy(0, StorageRecordStatus.DELETED, 10000);

        Assert.assertEquals(1000, activeEvents.size());
        Assert.assertEquals(0, deletedEvents.size());
    }

    private void generateEvents(int from, int to, int status, boolean expired) {
        for (int i = from; i <= to; i++) {
            Event event = new Event();
            event.eventTypeId = "event_" + i;
            event.trafficTypeName = "custom";
            event.key = "key1";

            long timestamp = System.currentTimeMillis() / 1000;
            long updatedAt = !expired ? timestamp : timestamp - EXPIRATION_PERIOD * 2;
            EventEntity entity = new EventEntity();
            entity.setCreatedAt(updatedAt);
            entity.setBody(Json.toJson(event));
            entity.setStatus(status);
            mRoomDb.eventDao().insert(entity);
        }
    }

    private List<Event> createEvents(int from, int to, int status) {
        List<Event> events = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            Event event = new Event();
            event.eventTypeId = "event_" + i;
            event.trafficTypeName = "custom";
            event.key = "key1";
            events.add(event);
        }
        return events;
    }

    private boolean checkStatus(List<EventEntity> entities, int status) {
        boolean statusOk = true;

        for (EventEntity entity : entities) {
            statusOk = statusOk && (entity.getStatus() == status);
        }
        return statusOk;
    }
}
