package database;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import helper.FileHelper;
import helper.IntegrationHelper;
import io.split.android.client.dtos.Event;
import io.split.android.client.storage.db.TrackEventEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;

public class TrackDaoTest {

    SplitRoomDatabase mRoomDb;
    Context mContext;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = SplitRoomDatabase.getDatabase(mContext, "encripted_api_key");
        mRoomDb.clearAllTables();
    }

    @Test
    public void insertRetrieve() throws InterruptedException {
        long timestamp = System.currentTimeMillis();
        List<TrackEventEntity> trackEvents = generateData(1, 10, timestamp, false);
        trackEvents.addAll(generateData(11, 15, timestamp, true));
        for(TrackEventEntity trackEvent : trackEvents) {
            mRoomDb.trackEventDao().insert(trackEvent);
        }

        List<TrackEventEntity> activeTrackEvents = mRoomDb.trackEventDao().getBy(timestamp, TrackEventEntity.STATUS_ACTIVE);
        List<TrackEventEntity> deletedTrackEvents = mRoomDb.trackEventDao().getBy(timestamp, TrackEventEntity.STATUS_DELETED);

        Assert.assertEquals(10, activeTrackEvents.size());
        Assert.assertEquals(5, deletedTrackEvents.size());
    }

    @Test
    public void insertUpdateRetrieve() throws InterruptedException {
        long timestamp = System.currentTimeMillis();
        List<TrackEventEntity> trackEvents = generateData(1, 20, timestamp, false);
        for(TrackEventEntity trackEvent : trackEvents) {
            mRoomDb.trackEventDao().insert(trackEvent);
        }

        List<TrackEventEntity> activeTrackEvents = mRoomDb.trackEventDao().getBy(timestamp, TrackEventEntity.STATUS_ACTIVE);
        List<Long> ids = activeTrackEvents.stream().map(TrackEventEntity::getId).collect(Collectors.toList());
        List<Long> idsToSoftDelete = ids.subList(15, 20);
        List<Long> idsToDelete = ids.subList(10, 15);

        mRoomDb.trackEventDao().markAsDeleted(idsToSoftDelete);
        List<TrackEventEntity> afterSoftDelete = mRoomDb.trackEventDao().getBy(0, TrackEventEntity.STATUS_ACTIVE);

        mRoomDb.trackEventDao().delete(idsToDelete);
        List<TrackEventEntity> afterDelete = mRoomDb.trackEventDao().getBy(0, TrackEventEntity.STATUS_ACTIVE);
        List<TrackEventEntity> softDeletedAfterDelete = mRoomDb.trackEventDao().getBy(0, TrackEventEntity.STATUS_DELETED);

        mRoomDb.trackEventDao().deleteOutdated(timestamp + 6);
        List<TrackEventEntity> afterAll = mRoomDb.trackEventDao().getBy(timestamp, TrackEventEntity.STATUS_ACTIVE);
        List<TrackEventEntity> deletedAfterAll = mRoomDb.trackEventDao().getBy(timestamp, TrackEventEntity.STATUS_DELETED);

        Assert.assertEquals(20, activeTrackEvents.size());
        Assert.assertEquals(15, afterSoftDelete.size());
        Assert.assertEquals(10, afterDelete.size());
        Assert.assertEquals(5, softDeletedAfterDelete.size());
        Assert.assertEquals(5, afterAll.size());
        Assert.assertEquals(5, deletedAfterAll.size());
    }

    @Test
    public void dataIntegrity() {
        long timestamp = System.currentTimeMillis();
        mRoomDb.trackEventDao().insert(generateData(1, 1, timestamp, false).get(0));
        mRoomDb.trackEventDao().insert(generateData(2, 2, timestamp, true).get(0));

        TrackEventEntity activeTrackEventEntity = mRoomDb.trackEventDao().getBy(timestamp, TrackEventEntity.STATUS_ACTIVE).get(0);
        TrackEventEntity deletedTrackEventEntity = mRoomDb.trackEventDao().getBy(timestamp, TrackEventEntity.STATUS_DELETED).get(0);

        Event activeTrackEvent = Json.fromJson(activeTrackEventEntity.getBody(), Event.class);
        Event deletedTrackEvent = Json.fromJson(deletedTrackEventEntity.getBody(), Event.class);
        
        Assert.assertEquals("type_1", activeTrackEvent.eventTypeId);
        Assert.assertEquals(1.0, activeTrackEvent.value, 0.0);
        Assert.assertEquals(timestamp + 1, activeTrackEvent.timestamp);
        Assert.assertEquals("traffic_1", activeTrackEvent.trafficTypeName);
        Assert.assertEquals("key", activeTrackEvent.key);
        Assert.assertEquals(TrackEventEntity.STATUS_ACTIVE, activeTrackEventEntity.getStatus());
        Assert.assertEquals(timestamp + 1, activeTrackEventEntity.getTimestamp());

        Assert.assertEquals("type_2", deletedTrackEvent.eventTypeId);
        Assert.assertEquals(2.0, deletedTrackEvent.value, 0.0);
        Assert.assertEquals(timestamp + 2, deletedTrackEvent.timestamp);
        Assert.assertEquals("traffic_2", deletedTrackEvent.trafficTypeName);
        Assert.assertEquals("key", deletedTrackEvent.key);
        Assert.assertEquals(TrackEventEntity.STATUS_DELETED, deletedTrackEventEntity.getStatus());
        Assert.assertEquals(timestamp + 2, deletedTrackEventEntity.getTimestamp());
    }

    @Test
    public void performance10() {
        performance(10);
    }

    @Test
    public void performance100() {
        performance(100);
    }

    @Test
    public void performance1000() {
        performance(1000);
    }

    @Test
    public void performance10000() {
        performance(10000);
    }

    private void performance(int count) {

        final String TAG = "TrackEventDaoTest_performance";

        List<TrackEventEntity> trackEventEntities = generateData(1, count, 100000, false);
        long start = System.currentTimeMillis();
        for(TrackEventEntity trackEventEntity : trackEventEntities) {
            mRoomDb.trackEventDao().insert(trackEventEntity);
        }
        long writeTime = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        trackEventEntities = mRoomDb.trackEventDao().getBy(0, TrackEventEntity.STATUS_ACTIVE);
        long readTime = System.currentTimeMillis() - start;

        IntegrationHelper.logSeparator(TAG);
        Log.i(TAG, "-> " +count  + " trackEvents");
        Log.i(TAG, String.format("Write time: %d segs, (%d millis) ", readTime / 100, readTime));
        Log.i(TAG, String.format("Read time: %d segs, (%d millis) ", writeTime / 100, writeTime));
        IntegrationHelper.logSeparator(TAG);

        Assert.assertEquals(count, trackEventEntities.size());
    }

    private List<TrackEventEntity> generateData(int from, int to, long timestamp, boolean markAsDeleted) {
        List<TrackEventEntity> trackEventList = new ArrayList<>();
        for(int i = from; i<=to; i++) {
            Event trackEvent = new Event();
            trackEvent.trafficTypeName = "traffic_" + i;
            trackEvent.eventTypeId = "type_" + i;
            trackEvent.key = "key";
            trackEvent.value = i;
            trackEvent.timestamp = timestamp + i;

            TrackEventEntity trackEventEntity = new TrackEventEntity();
            trackEventEntity.setBody(Json.toJson(trackEvent));
            trackEventEntity.setTimestamp(timestamp + i);
            trackEventEntity.setStatus(!markAsDeleted ? TrackEventEntity.STATUS_ACTIVE : TrackEventEntity.STATUS_DELETED);
            trackEventList.add(trackEventEntity);
        }
        return trackEventList;
    }
}
