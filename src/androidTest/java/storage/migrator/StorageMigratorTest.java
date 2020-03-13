package storage.migrator;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.storage.db.migrator.StorageMigrator;
import io.split.android.client.utils.Json;
import storage.migrator.mocks.EventsMigratorHelperMock;
import storage.migrator.mocks.ImpressionsMigratorHelperMock;
import storage.migrator.mocks.MySegmentsMigratorHelperMock;
import storage.migrator.mocks.SplitsMigratorHelperMock;


public class StorageMigratorTest {

    MySegmentsMigratorHelperMock mMySegmentsMigratorHelper;
    SplitsMigratorHelperMock mSplitsMigratorHelper;
    EventsMigratorHelperMock mEventsMigratorHelper;
    ImpressionsMigratorHelperMock mImpressionsMigratorHelper;
    StorageMigrator mMigrator;
    SplitRoomDatabase mDatabase;

    @Before
    public void setup() {

        mMySegmentsMigratorHelper = new MySegmentsMigratorHelperMock();
        mSplitsMigratorHelper = new SplitsMigratorHelperMock();
        mEventsMigratorHelper = new EventsMigratorHelperMock();
        mImpressionsMigratorHelper = new ImpressionsMigratorHelperMock();

        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        context.deleteDatabase("migrator_folder_test");
        mDatabase = SplitRoomDatabase.getDatabase(context,
                "migrator_folder_test");
        mDatabase.clearAllTables();
        mMigrator = new StorageMigrator(mDatabase);
    }

    @Test
    public void successfulMigration() {
        mMySegmentsMigratorHelper.setMySegments(generateMySegmentEntities(10));
        mSplitsMigratorHelper.setSnapshot(100L, generateSplitsEntities(10));
        mEventsMigratorHelper.setEvents(generateEventsEntities(10));
        mImpressionsMigratorHelper.setImpressions(generateImpressionsEntities(10));

        mMigrator.runMigration(mMySegmentsMigratorHelper, mSplitsMigratorHelper,
                mEventsMigratorHelper, mImpressionsMigratorHelper);

        MySegmentEntity mySegmentEntity3 = mDatabase.mySegmentDao().getByUserKeys("the_key_3");
        MySegmentEntity mySegmentEntity8 = mDatabase.mySegmentDao().getByUserKeys("the_key_8");
        GeneralInfoEntity migrationInfo = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.DATBASE_MIGRATION_STATUS);

        List<SplitEntity> splitEntities = mDatabase.splitDao().getAll();
        SplitEntity splitEntity = findSplitByName("split_1", splitEntities);
        Split split = Json.fromJson(splitEntity.getBody(), Split.class);

        List<EventEntity> eventEntities = mDatabase.eventDao().getBy(0, StorageRecordStatus.ACTIVE, 1000);
        EventEntity eventEntity = findEventByType("event_1", eventEntities);
        Event event = Json.fromJson(eventEntity.getBody(), Event.class);

        List<ImpressionEntity> impressionEntities = mDatabase.impressionDao().getBy(0, StorageRecordStatus.ACTIVE, 1000);
        ImpressionEntity impressionEntity = findImpressionsByTestName("feature_4", impressionEntities);
        KeyImpression impression = Json.fromJson(impressionEntity.getBody(), KeyImpression.class);

        Assert.assertEquals(GeneralInfoEntity.DATBASE_MIGRATION_STATUS_DONE, migrationInfo.getLongValue());
        Assert.assertNotNull(mySegmentEntity3);
        Assert.assertEquals("the_key_3", mySegmentEntity3.getUserKey());
        Assert.assertEquals("segment1,segment2,segment3", mySegmentEntity3.getSegmentList());
        Assert.assertEquals(100, mySegmentEntity3.getUpdatedAt());

        Assert.assertNotNull(mySegmentEntity8);
        Assert.assertEquals("the_key_8", mySegmentEntity8.getUserKey());
        Assert.assertEquals("segment10,segment20,segment30", mySegmentEntity8.getSegmentList());
        Assert.assertEquals(100, mySegmentEntity8.getUpdatedAt());

        Assert.assertEquals(10, splitEntities.size());
        Assert.assertEquals(100, splitEntity.getUpdatedAt());
        Assert.assertEquals("split_1", splitEntity.getName());
        Assert.assertEquals("split_1", split.name);
        Assert.assertEquals(Status.ACTIVE, split.status);

        Assert.assertEquals(10, eventEntities.size());
        Assert.assertEquals(100, eventEntity.getCreatedAt());
        Assert.assertEquals(StorageRecordStatus.ACTIVE, eventEntity.getStatus());
        Assert.assertEquals("event_1", event.eventTypeId);
        Assert.assertEquals("the_key_1", event.key);
        Assert.assertEquals("custom", event.trafficTypeName);
        Assert.assertEquals(1.0, event.value, 0);
        Assert.assertEquals("custom", (String)event.properties.get("pepe"));

        Assert.assertEquals(10, impressionEntities.size());
        Assert.assertEquals(100, impressionEntity.getCreatedAt());
        Assert.assertEquals(StorageRecordStatus.ACTIVE, impressionEntity.getStatus());
        Assert.assertEquals("feature_4", impressionEntity.getTestName());
        Assert.assertEquals("feature_4", impression.feature);
        Assert.assertEquals("the_key_4", impression.keyName);
        Assert.assertEquals("on", impression.treatment);
        Assert.assertEquals("custom", (String)event.properties.get("pepe"));

    }

    @Test
    public void emptyMigration() {
        mMySegmentsMigratorHelper.setMySegments(new ArrayList<>());
        mSplitsMigratorHelper.setSnapshot(-1, new ArrayList<>());
        mEventsMigratorHelper.setEvents(new ArrayList<>());
        mImpressionsMigratorHelper.setImpressions(new ArrayList<>());

        mMigrator.runMigration(mMySegmentsMigratorHelper, mSplitsMigratorHelper,
                mEventsMigratorHelper, mImpressionsMigratorHelper);

        List<SplitEntity> splitEntities = mDatabase.splitDao().getAll();
        List<EventEntity> eventEntities = mDatabase.eventDao().getBy(0, StorageRecordStatus.ACTIVE, 1000);
        List<ImpressionEntity> impressionEntities = mDatabase.impressionDao().getBy(0, StorageRecordStatus.ACTIVE, 1000);
        GeneralInfoEntity migrationInfo = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.DATBASE_MIGRATION_STATUS);

        Assert.assertEquals(GeneralInfoEntity.DATBASE_MIGRATION_STATUS_DONE, migrationInfo.getLongValue());
        Assert.assertEquals(0, splitEntities.size());
        Assert.assertEquals(0, eventEntities.size());
        Assert.assertEquals(0, impressionEntities.size());
    }

    @Test
    public void failedMigration() {
        mMySegmentsMigratorHelper.setMySegments(null);
        mSplitsMigratorHelper.setSnapshot(-1, null);
        mEventsMigratorHelper.setEvents(null);
        mImpressionsMigratorHelper.setImpressions(null);

        mMigrator.runMigration(mMySegmentsMigratorHelper, mSplitsMigratorHelper,
                mEventsMigratorHelper, mImpressionsMigratorHelper);

        List<SplitEntity> splitEntities = mDatabase.splitDao().getAll();
        List<EventEntity> eventEntities = mDatabase.eventDao().getBy(0, StorageRecordStatus.ACTIVE, 1000);
        List<ImpressionEntity> impressionEntities = mDatabase.impressionDao().getBy(0, StorageRecordStatus.ACTIVE, 1000);
        GeneralInfoEntity migrationInfo = mDatabase.generalInfoDao().getByName(GeneralInfoEntity.DATBASE_MIGRATION_STATUS);

        Assert.assertEquals(GeneralInfoEntity.DATBASE_MIGRATION_STATUS_DONE, migrationInfo.getLongValue());
        Assert.assertEquals(0, splitEntities.size());
        Assert.assertEquals(0, eventEntities.size());
        Assert.assertEquals(0, impressionEntities.size());
    }

    private List<MySegmentEntity> generateMySegmentEntities(int count) {
        List<MySegmentEntity> entities = new ArrayList<>();
        for(int i=0; i<count; i++) {
            MySegmentEntity entity = new MySegmentEntity();
            entity.setUserKey("the_key_" + i);
            String segments = "segment1,segment2,segment3";
            if(i > 4) {
                segments = "segment10,segment20,segment30";
            }
            entity.setUpdatedAt(100);
            entity.setSegmentList(segments);
            entities.add(entity);
        }
        return entities;
    }

    private List<SplitEntity> generateSplitsEntities(int count) {
        List<SplitEntity> entities = new ArrayList<>();
        for(int i=0; i<count; i++) {
            Split split = new Split();
            split.name = "split_" + i;
            split.status = Status.ACTIVE;
            SplitEntity entity = new SplitEntity();
            entity.setName(split.name);
            entity.setUpdatedAt(100);
            entity.setBody(Json.toJson(split));
            entities.add(entity);
        }
        return entities;
    }

    private List<EventEntity> generateEventsEntities(int count) {
        List<EventEntity> entities = new ArrayList<>();
        for(int i=0; i<count; i++) {
            Event event = new Event();
            event.key = "the_key_" + i;
            event.eventTypeId = "event_" + i;
            event.trafficTypeName = "custom";
            event.timestamp = 100;
            event.value = i;
            event.properties = new HashMap<>();
            event.properties.put("pepe", "custom");
            EventEntity entity = new EventEntity();
            entity.setStatus(StorageRecordStatus.ACTIVE);
            entity.setCreatedAt(100);
            entity.setBody(Json.toJson(event));
            entities.add(entity);
        }
        return entities;
    }

    private List<ImpressionEntity> generateImpressionsEntities(int count) {
        List<ImpressionEntity> entities = new ArrayList<>();
        for(int i=0; i<count; i++) {
            KeyImpression impression = new KeyImpression();
            impression.keyName = "the_key_" + i;
            impression.treatment = "on";
            impression.feature = "feature_" + i;
            impression.time = 100;
            impression.label = "label";
            ImpressionEntity entity = new ImpressionEntity();
            entity.setTestName(impression.feature);
            entity.setStatus(StorageRecordStatus.ACTIVE);
            entity.setCreatedAt(100);
            entity.setBody(Json.toJson(impression));
            entities.add(entity);
        }
        return entities;
    }

    private ImpressionEntity findImpressionsByTestName(String testName, List<ImpressionEntity> entities) {
        return entities.stream().filter(impression ->
                impression.getTestName().equals(testName))
                .collect(Collectors.toList()).get(0);
    }

    private EventEntity findEventByType(String type, List<EventEntity> entities) {
        return entities.stream().filter(event ->
                event.getBody().contains(type))
                .collect(Collectors.toList()).get(0);
    }

    private SplitEntity findSplitByName(String name, List<SplitEntity> entities) {
        return entities.stream().filter(split ->
                split.getBody().contains(name))
                .collect(Collectors.toList()).get(0);
    }
}
