package tests.storage.migrator;

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
import io.split.android.client.cache.MySegmentsCache;
import io.split.android.client.cache.MySegmentsCacheMigrator;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.storage.legacy.IStorage;
import io.split.android.client.utils.StringHelper;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MySegmentsMigratorHelperTest {
    FileHelper mFileHelper = new FileHelper();
    MySegmentsMigratorHelper mMigrator;
    MySegmentsCache mMySegmentsCache;

    @Before
    public void setup() {
        File cacheFolder = InstrumentationRegistry.getInstrumentation().getContext().getCacheDir();
        File rootFolder = mFileHelper.emptyAndGetTestFolder(cacheFolder, "myseg_folder_test");

        IStorage fileStorage = new FileStorage(rootFolder, "mysegments");
        mMySegmentsCache = new MySegmentsCache(fileStorage);
        MySegmentsCacheMigrator mySegmentsCacheMigrator = mMySegmentsCache;
        mMigrator = new MySegmentsMigratorHelperImpl(mMySegmentsCache, new StringHelper());
    }

    @Test
    public void basicMigration() {

        mMySegmentsCache.setMySegments("key1", createMySegments("key1", 1, 10));
        mMySegmentsCache.setMySegments("key2", createMySegments("key2", 11, 15));
        mMySegmentsCache.setMySegments("key3", createMySegments("key3", 21, 35));

        List<MySegmentEntity> entities = mMigrator.loadLegacySegmentsAsEntities();
        Map<String, MySegmentEntity> entityMap = mySegmentEntityMap(entities);

        MySegmentEntity entity1 = entityMap.get("key1");
        MySegmentEntity entity2 = entityMap.get("key2");
        MySegmentEntity entity3 = entityMap.get("key3");

        Assert.assertEquals(3, entities.size());
        Assert.assertNotNull(entity1);
        Assert.assertNotNull(entity2);
        Assert.assertNotNull(entity3);

        Assert.assertTrue(entity1.getSegmentList().contains("segment_key1_1"));
        Assert.assertTrue(entity1.getSegmentList().contains("segment_key1_10"));
        Assert.assertEquals("key1", entity1.getUserKey());
        Assert.assertTrue(entity1.getUpdatedAt() > 0);

        Assert.assertTrue(entity2.getSegmentList().contains("segment_key2_11"));
        Assert.assertTrue(entity2.getSegmentList().contains("segment_key2_15"));
        Assert.assertEquals("key2", entity2.getUserKey());
        Assert.assertTrue(entity2.getUpdatedAt() > 0);

        Assert.assertTrue(entity3.getSegmentList().contains("segment_key3_21"));
        Assert.assertTrue(entity3.getSegmentList().contains("segment_key3_35"));
        Assert.assertEquals("key3", entity3.getUserKey());
        Assert.assertTrue(entity3.getUpdatedAt() > 0);
    }

    @Test
    public void emptyMigration() {
        List<MySegmentEntity> entities = mMigrator.loadLegacySegmentsAsEntities();
        Assert.assertEquals(0, entities.size());
    }

    private List<MySegment> createMySegments(String key, int from, int to) {
        List<MySegment> mySegments = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            MySegment mySegment = new MySegment();
            mySegment.id = "id_" + i;
            mySegment.name = "segment_" + key + "_" + i;
            mySegments.add(mySegment);
        }
        return mySegments;
    }

    private Map<String, MySegmentEntity> mySegmentEntityMap(List<MySegmentEntity> entities) {
        Map<String, MySegmentEntity> entityMap = new HashMap<>();
        for (MySegmentEntity entity : entities) {
            entityMap.put(entity.getUserKey(), entity);
        }
        return entityMap;
    }
}
