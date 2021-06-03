package tests.storage;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.service.impressions.ImpressionsCountPerFeature;
import io.split.android.client.storage.db.ImpressionsCountEntity;
import io.split.android.client.storage.db.ImpressionsCountEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;

import io.split.android.client.storage.impressions.PersistentImpressionsCountStorage;
import io.split.android.client.storage.impressions.SqLitePersistentImpressionsCountStorage;
import io.split.android.client.utils.Json;

public class PersistentImpressionCountStorageTest {

    final static long EXPIRATION_PERIOD = 3600 * 24;
    SplitRoomDatabase mRoomDb;
    Context mContext;
    PersistentImpressionsCountStorage mStorage;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mContext.deleteDatabase("encripted_api_key");
        mRoomDb = SplitRoomDatabase.getDatabase(mContext, "encripted_api_key");
        mRoomDb.clearAllTables();
        generateImpressionsCount(1, 10, StorageRecordStatus.ACTIVE, false);
        generateImpressionsCount(101, 110, StorageRecordStatus.DELETED, false);
        generateImpressionsCount(301, 310, StorageRecordStatus.ACTIVE, true);

        mStorage = new SqLitePersistentImpressionsCountStorage(mRoomDb, EXPIRATION_PERIOD);
    }

    @Test
    public void create() {
        List<ImpressionsCountPerFeature> ImpressionsCount = createImpressionsCount(201, 210, StorageRecordStatus.ACTIVE);
        for (ImpressionsCountPerFeature count : ImpressionsCount) {
            mStorage.push(count);
        }
        List<ImpressionsCountEntity> first10ActiveLoadedCount = mRoomDb.impressionsCountDao().getBy(0,
                StorageRecordStatus.ACTIVE, 10);
        List<ImpressionsCountEntity> allActiveLoadedImpressionsCount = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.ACTIVE, 100);
        List<ImpressionsCountEntity> first5DeletedLoadedCount = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.DELETED, 5);
        List<ImpressionsCountEntity> allDeletedLoadedCount = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.DELETED, 100);
        List<ImpressionsCountEntity> noCount = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.DELETED, 0);

        boolean first10ActiveStatus = checkStatus(first10ActiveLoadedCount, StorageRecordStatus.ACTIVE);
        boolean allActiveStatus = checkStatus(allActiveLoadedImpressionsCount, StorageRecordStatus.ACTIVE);
        boolean first5DeletedStatus = checkStatus(first5DeletedLoadedCount, StorageRecordStatus.DELETED);
        boolean allDeletedStatus = checkStatus(allDeletedLoadedCount, StorageRecordStatus.DELETED);

        ImpressionsCountPerFeature firstActiveCount = Json.fromJson(first10ActiveLoadedCount.get(0).getBody(), ImpressionsCountPerFeature.class);
        ImpressionsCountPerFeature lastActiveCount = Json.fromJson(first10ActiveLoadedCount.get(9).getBody(), ImpressionsCountPerFeature.class);

        ImpressionsCountPerFeature firstAllActiveCount = Json.fromJson(allActiveLoadedImpressionsCount.get(0).getBody(), ImpressionsCountPerFeature.class);
        ImpressionsCountPerFeature lastAllActiveCount = Json.fromJson(allActiveLoadedImpressionsCount.get(19).getBody(), ImpressionsCountPerFeature.class);

        ImpressionsCountPerFeature first5DeletedCount = Json.fromJson(first5DeletedLoadedCount.get(0).getBody(), ImpressionsCountPerFeature.class);
        ImpressionsCountPerFeature last5DeletedCount = Json.fromJson(first5DeletedLoadedCount.get(4).getBody(), ImpressionsCountPerFeature.class);

        ImpressionsCountPerFeature firstAllDeletedCount = Json.fromJson(allDeletedLoadedCount.get(0).getBody(), ImpressionsCountPerFeature.class);
        ImpressionsCountPerFeature lastAllDeletedCount = Json.fromJson(allDeletedLoadedCount.get(9).getBody(), ImpressionsCountPerFeature.class);

        Assert.assertEquals(10, first10ActiveLoadedCount.size());
        Assert.assertEquals(30, allActiveLoadedImpressionsCount.size());
        Assert.assertEquals(5, first5DeletedLoadedCount.size());
        Assert.assertEquals(10, allDeletedLoadedCount.size());
        Assert.assertEquals(0, noCount.size());

        Assert.assertTrue(first10ActiveStatus);
        Assert.assertTrue(allActiveStatus);
        Assert.assertTrue(first5DeletedStatus);
        Assert.assertTrue(allDeletedStatus);

        Assert.assertEquals("feature_301", firstActiveCount.feature);
        Assert.assertEquals("feature_310", lastActiveCount.feature);

        Assert.assertEquals("feature_301", firstAllActiveCount.feature);
        Assert.assertEquals("feature_10", lastAllActiveCount.feature);

        Assert.assertEquals("feature_101", first5DeletedCount.feature);
        Assert.assertEquals("feature_105", last5DeletedCount.feature);

        Assert.assertEquals("feature_101", firstAllDeletedCount.feature);
        Assert.assertEquals("feature_110", lastAllDeletedCount.feature);

    }

    @Test
    public void pop() {

        List<ImpressionsCountPerFeature> count1 = mStorage.pop(5);
        List<ImpressionsCountPerFeature> count2 = mStorage.pop(100);
        List<ImpressionsCountEntity> activeImpressionsCount = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.ACTIVE, 100);
        List<ImpressionsCountEntity> deletedImpressionsCount = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.DELETED, 100);

        Assert.assertEquals(5, count1.size());
        Assert.assertEquals(5, count2.size());
        Assert.assertEquals("feature_1", count1.get(0).feature);
        Assert.assertEquals("feature_5", count1.get(4).feature);
        Assert.assertEquals(10, activeImpressionsCount.size());
        Assert.assertEquals(20, deletedImpressionsCount.size());
    }

    @Test
    public void popMasive() {
        // To make sure that popping in chunks works as expected
        mRoomDb.clearAllTables();
        List<ImpressionsCountPerFeature> ImpressionsCount = createImpressionsCount(1000, 6000, StorageRecordStatus.ACTIVE);
        for(ImpressionsCountPerFeature event : ImpressionsCount) {
            mStorage.push(event);
        }
        List<ImpressionsCountPerFeature> Impressions1 = mStorage.pop(2000);
        List<ImpressionsCountPerFeature> Impressions2 = mStorage.pop(2001);
        List<ImpressionsCountEntity> activeImpressions = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.ACTIVE, 10000);
        List<ImpressionsCountEntity> deletedImpressions = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.DELETED, 10000);

        Assert.assertEquals(2000, Impressions1.size());
        Assert.assertEquals(2001, Impressions2.size());
        Assert.assertEquals(4001, deletedImpressions.size());
        Assert.assertEquals(1000, activeImpressions.size());
    }

    @Test
    public void setActive() {

        List<ImpressionsCountPerFeature> ImpressionsCount = mStorage.pop(100);
        List<ImpressionsCountEntity> activeImpressionsCountBefore = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.ACTIVE, 100);
        List<ImpressionsCountEntity> deletedImpressionsCountBefore = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.DELETED, 100);
        mStorage.setActive(ImpressionsCount);
        List<ImpressionsCountEntity> activeImpressionsCount = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.ACTIVE, 100);
        List<ImpressionsCountEntity> deletedImpressionsCountAfter = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.DELETED, 100);

        Assert.assertEquals(10, ImpressionsCount.size());
        Assert.assertEquals(10, activeImpressionsCountBefore.size());
        Assert.assertEquals(20, deletedImpressionsCountBefore.size());
        Assert.assertEquals(20, activeImpressionsCount.size());
        Assert.assertEquals(10, deletedImpressionsCountAfter.size());
    }

    @Test
    public void setActiveMasive() {

        mRoomDb.clearAllTables();
        List<ImpressionsCountPerFeature> masiveImpressionsCount = createImpressionsCount(1, 4000, StorageRecordStatus.ACTIVE);
        for(ImpressionsCountPerFeature event : masiveImpressionsCount) {
            mStorage.push(event);
        }

        List<ImpressionsCountPerFeature> ImpressionsCount = mStorage.pop(3001);
        List<ImpressionsCountEntity> activeImpressionsCountBefore = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.ACTIVE, 10000);
        List<ImpressionsCountEntity> deletedImpressionsCountBefore = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.DELETED, 10000);
        mStorage.setActive(ImpressionsCount);
        List<ImpressionsCountEntity> activeImpressionsCount = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.ACTIVE, 10000);
        List<ImpressionsCountEntity> deletedImpressionsCountAfter = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.DELETED, 10000);

        Assert.assertEquals(3001, ImpressionsCount.size());
        Assert.assertEquals(999, activeImpressionsCountBefore.size());
        Assert.assertEquals(3001, deletedImpressionsCountBefore.size());
        Assert.assertEquals(4000, activeImpressionsCount.size());
        Assert.assertEquals(0, deletedImpressionsCountAfter.size());
    }

    @Test
    public void masiveDelete() {

        mRoomDb.clearAllTables();
        List<ImpressionsCountPerFeature> masiveImpressionsCount = createImpressionsCount(1, 4000, StorageRecordStatus.ACTIVE);
        for(ImpressionsCountPerFeature event : masiveImpressionsCount) {
            mStorage.push(event);
        }

        List<ImpressionsCountPerFeature> toDelete =  mStorage.pop(3000);
        mStorage.setActive(toDelete);

        mStorage.delete(toDelete);
        List<ImpressionsCountEntity> activeImpressionsCount = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.ACTIVE, 10000);
        List<ImpressionsCountEntity> deletedImpressionsCount = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.DELETED, 10000);

        Assert.assertEquals(1000, activeImpressionsCount.size());
        Assert.assertEquals(0, deletedImpressionsCount.size());
    }

    @Test
    public void deleteMasiveOutdated() {

        mRoomDb.clearAllTables();
        List<ImpressionsCountPerFeature> masiveImpressionsCount = createImpressionsCount(1, 6000, StorageRecordStatus.ACTIVE);
        for(ImpressionsCountPerFeature event : masiveImpressionsCount) {
            mStorage.push(event);
        }

        List<ImpressionsCountPerFeature> toDelete =  mStorage.pop(5000);

        mStorage.deleteInvalid((System.currentTimeMillis() / 1000) + 2000);
        List<ImpressionsCountEntity> activeImpressionsCount = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.ACTIVE, 10000);
        List<ImpressionsCountEntity> deletedImpressionsCount = mRoomDb.impressionsCountDao().getBy(0, StorageRecordStatus.DELETED, 10000);

        Assert.assertEquals(1000, activeImpressionsCount.size());
        Assert.assertEquals(0, deletedImpressionsCount.size());
    }

    private void generateImpressionsCount(int from, int to, int status, boolean expired) {
        for (int i = from; i <= to; i++) {
            ImpressionsCountPerFeature count = new ImpressionsCountPerFeature("feature_" + i, i, i);

            long timestamp = System.currentTimeMillis() / 1000;
            long updatedAt = !expired ? timestamp : timestamp - EXPIRATION_PERIOD * 2;
            ImpressionsCountEntity entity = new ImpressionsCountEntity();
            entity.setCreatedAt(updatedAt);
            entity.setBody(Json.toJson(count));
            entity.setStatus(status);
            mRoomDb.impressionsCountDao().insert(entity);
        }
    }

    private List<ImpressionsCountPerFeature> createImpressionsCount(int from, int to, int status) {
        List<ImpressionsCountPerFeature> counts = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            ImpressionsCountPerFeature count = new ImpressionsCountPerFeature("feature_" + i, i, i);
            counts.add(count);
        }
        return counts;
    }

    private boolean checkStatus(List<ImpressionsCountEntity> entities, int status) {
        boolean statusOk = true;

        for (ImpressionsCountEntity entity : entities) {
            statusOk = statusOk && (entity.getStatus() == status);
        }
        return statusOk;
    }
}