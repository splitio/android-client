package storage;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.impressions.SqLitePersistentImpressionsStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.StringHelper;

public class PersistentImpressionStorageTest {

    SplitRoomDatabase mRoomDb;
    Context mContext;
    PersistentImpressionsStorage mPersistentImpressionStorage;
    final static long EXPIRATION_PERIOD = 3600 * 24;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mContext.deleteDatabase("encripted_api_key");
        mRoomDb = SplitRoomDatabase.getDatabase(mContext, "encripted_api_key");
        mRoomDb.clearAllTables();
        generateImpressions(1, 10, StorageRecordStatus.ACTIVE, false);
        generateImpressions(101, 110, StorageRecordStatus.DELETED, false);
        generateImpressions(301, 310, StorageRecordStatus.ACTIVE, true);

        mPersistentImpressionStorage = new SqLitePersistentImpressionsStorage(mRoomDb, EXPIRATION_PERIOD);
    }

    @Test
    public void create() {
        List<KeyImpression> impressions = createImpressions(201, 210, StorageRecordStatus.ACTIVE);
        for(KeyImpression Impression : impressions) {
            mPersistentImpressionStorage.push(Impression);
        }
        List<ImpressionEntity> first10ActiveLoadedImpressions = mRoomDb.impressionDao().getBy(0,
                StorageRecordStatus.ACTIVE, 10);
        List<ImpressionEntity> allActiveLoadedImpressions = mRoomDb.impressionDao().getBy(0, StorageRecordStatus.ACTIVE, 100);
        List<ImpressionEntity> first5DeletedLoadedImpressions = mRoomDb.impressionDao().getBy(0, StorageRecordStatus.DELETED, 5);
        List<ImpressionEntity> allDeletedLoadedImpressions = mRoomDb.impressionDao().getBy(0, StorageRecordStatus.DELETED, 100);
        List<ImpressionEntity> noImpressions = mRoomDb.impressionDao().getBy(0, StorageRecordStatus.DELETED, 0);

        boolean first10ActiveStatus = checkStatus(first10ActiveLoadedImpressions, StorageRecordStatus.ACTIVE);
        boolean allActiveStatus = checkStatus(allActiveLoadedImpressions, StorageRecordStatus.ACTIVE);
        boolean first5DeletedStatus = checkStatus(first5DeletedLoadedImpressions, StorageRecordStatus.DELETED);
        boolean allDeletedStatus = checkStatus(allDeletedLoadedImpressions, StorageRecordStatus.DELETED);

        KeyImpression firstActiveImpression = Json.fromJson(first10ActiveLoadedImpressions.get(0).getBody(), KeyImpression.class);
        KeyImpression lastActiveImpression = Json.fromJson(first10ActiveLoadedImpressions.get(9).getBody(), KeyImpression.class);

        KeyImpression firstAllActiveImpression = Json.fromJson(allActiveLoadedImpressions.get(0).getBody(), KeyImpression.class);
        KeyImpression lastAllActiveImpression = Json.fromJson(allActiveLoadedImpressions.get(19).getBody(), KeyImpression.class);

        KeyImpression first5DeletedImpression = Json.fromJson(first5DeletedLoadedImpressions.get(0).getBody(), KeyImpression.class);
        KeyImpression last5DeletedImpression = Json.fromJson(first5DeletedLoadedImpressions.get(4).getBody(), KeyImpression.class);

        KeyImpression firstAllDeletedImpression = Json.fromJson(allDeletedLoadedImpressions.get(0).getBody(), KeyImpression.class);
        KeyImpression lastAllDeletedImpression = Json.fromJson(allDeletedLoadedImpressions.get(9).getBody(), KeyImpression.class);

        Assert.assertEquals(10, first10ActiveLoadedImpressions.size());
        Assert.assertEquals(30, allActiveLoadedImpressions.size());
        Assert.assertEquals(5, first5DeletedLoadedImpressions.size());
        Assert.assertEquals(10, allDeletedLoadedImpressions.size());
        Assert.assertEquals(0, noImpressions.size());

        Assert.assertTrue(first10ActiveStatus);
        Assert.assertTrue(allActiveStatus);
        Assert.assertTrue(first5DeletedStatus);
        Assert.assertTrue(allDeletedStatus);

        Assert.assertEquals("Impression_301", firstActiveImpression.keyName);
        Assert.assertEquals("Impression_310", lastActiveImpression.keyName);

        Assert.assertEquals("Impression_301", firstAllActiveImpression.keyName);
        Assert.assertEquals("Impression_10", lastAllActiveImpression.keyName);

        Assert.assertEquals("Impression_101", first5DeletedImpression.keyName);
        Assert.assertEquals("Impression_105", last5DeletedImpression.keyName);

        Assert.assertEquals("Impression_101", firstAllDeletedImpression.keyName);
        Assert.assertEquals("Impression_110", lastAllDeletedImpression.keyName);

    }

    @Test
    public void pop() {

        List<KeyImpression> Impressions1 = mPersistentImpressionStorage.pop(5);
        List<KeyImpression> Impressions2 = mPersistentImpressionStorage.pop(100);
        List<ImpressionEntity> activeImpressions = mRoomDb.impressionDao().getBy(0, StorageRecordStatus.ACTIVE, 100);
        List<ImpressionEntity> deletedImpressions = mRoomDb.impressionDao().getBy(0, StorageRecordStatus.DELETED, 100);

        Assert.assertEquals(5, Impressions1.size());
        Assert.assertEquals(5, Impressions2.size());
        Assert.assertEquals("Impression_1", Impressions1.get(0).keyName);
        Assert.assertEquals("Impression_5", Impressions1.get(4).keyName);
        Assert.assertEquals(10, activeImpressions.size());
        Assert.assertEquals(20, deletedImpressions.size());
    }

    @Test
    public void setActive() {

        List<KeyImpression> impressions = mPersistentImpressionStorage.pop(100);
        List<ImpressionEntity> activeImpressionsBefore = mRoomDb.impressionDao().getBy(0, StorageRecordStatus.ACTIVE, 100);
        List<ImpressionEntity> deletedImpressionsBefore = mRoomDb.impressionDao().getBy(0, StorageRecordStatus.DELETED, 100);
        mPersistentImpressionStorage.setActive(impressions);
        List<ImpressionEntity> activeImpressions = mRoomDb.impressionDao().getBy(0, StorageRecordStatus.ACTIVE, 100);
        List<ImpressionEntity> deletedImpressionsAfter = mRoomDb.impressionDao().getBy(0, StorageRecordStatus.DELETED, 100);

        Assert.assertEquals(10, impressions.size());
        Assert.assertEquals(10, activeImpressionsBefore.size());
        Assert.assertEquals(20, deletedImpressionsBefore.size());
        Assert.assertEquals(20, activeImpressions.size());
        Assert.assertEquals(10, deletedImpressionsAfter.size());
    }

    private void generateImpressions(int from, int to, int status, boolean expired) {
        for(int i = from; i <= to; i++) {
            long timestamp  = System.currentTimeMillis() / 1000;
            long createdAt = !expired ? timestamp : timestamp - EXPIRATION_PERIOD * 2;
            ImpressionEntity entity = new ImpressionEntity();
            entity.setCreatedAt(createdAt);
            entity.setBody(Json.toJson(newImpression(i)));
            entity.setTestName("feature_" + i);
            entity.setStatus(status);
            mRoomDb.impressionDao().insert(entity);
        }
    }

    private List<KeyImpression> createImpressions(int from, int to, int status) {
        List<KeyImpression> impressions = new ArrayList<>();
        for(int i = from; i <= to; i++) {
            impressions.add(newImpression(i));
        }
        return impressions;
    }

    private KeyImpression newImpression(int i) {
        KeyImpression impression = new KeyImpression();
        impression.keyName = "Impression_" + i;
        impression.feature = "feature_" + i;
        impression.time = 11111;
        impression.changeNumber = 9999L;
        impression.label  = "default rule";
        return impression;
    }

    private boolean checkStatus(List<ImpressionEntity> entities, int status) {
        boolean statusOk = true;

        for(ImpressionEntity entity : entities) {
            statusOk = statusOk && (entity.getStatus() == status);
        }
        return statusOk;
    }
}