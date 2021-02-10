package tests.storage.migrator;

import androidx.core.util.Pair;
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
import io.split.android.client.cache.SplitCache;
import io.split.android.client.cache.SplitCacheMigrator;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.storage.legacy.IStorage;
import io.split.android.client.utils.Json;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class SplitsMigratorHelperTest {
    FileHelper mFileHelper = new FileHelper();
    SplitsMigratorHelper mMigrator;
    SplitCache mSplitsCache;

    @Before
    public void setup() {
        File cacheFolder = InstrumentationRegistry.getInstrumentation().getContext().getCacheDir();
        File rootFolder = mFileHelper.emptyAndGetTestFolder(cacheFolder, "splits_folder_test");

        IStorage fileStorage = new FileStorage(rootFolder, "splits");
        mSplitsCache = new SplitCache(fileStorage);
        SplitCacheMigrator splitsCacheMigrator = mSplitsCache;
        mMigrator = new SplitsMigratorHelperImpl(mSplitsCache);
    }

    @Test
    public void basicMigration() {

        List<Split> splits = createSplits(1, 10);
        for(Split split : splits) {
            mSplitsCache.addSplit(split);
        }

        Pair<Long, List<SplitEntity>> splitSnapshot = mMigrator.loadLegacySplitsAsEntities();
        List<SplitEntity> entities = splitSnapshot.second;
        Map<String, SplitEntity> entityMap = splitsEntityMap(splitSnapshot.second);

        SplitEntity entity1 = entityMap.get("split_1");
        SplitEntity entity2 = entityMap.get("split_5");
        SplitEntity entity3 = entityMap.get("split_10");

        Split split1 = Json.fromJson(entity1.getBody(), Split.class);
        Split split2 = Json.fromJson(entity2.getBody(), Split.class);
        Split split3 = Json.fromJson(entity3.getBody(), Split.class);

        Assert.assertEquals(10, entities.size());

        Assert.assertNotNull(entity1);
        Assert.assertNotNull(entity2);
        Assert.assertNotNull(entity3);

        Assert.assertEquals("split_1", entity1.getName());
        Assert.assertEquals("split_1", split1.name);
        Assert.assertEquals(Status.ACTIVE, split1.status);
        Assert.assertTrue(entity1.getUpdatedAt() > 0);

        Assert.assertEquals("split_5", entity2.getName());
        Assert.assertEquals("split_5", split2.name);
        Assert.assertEquals(Status.ACTIVE, split2.status);
        Assert.assertTrue(entity2.getUpdatedAt() > 0);

        Assert.assertEquals("split_10", entity3.getName());
        Assert.assertEquals("split_10", split3.name);
        Assert.assertEquals(Status.ACTIVE, split3.status);
        Assert.assertTrue(entity3.getUpdatedAt() > 0);

    }

    @Test
    public void emptyMigration() {
        Pair<Long, List<SplitEntity>> splitSnapshot = mMigrator.loadLegacySplitsAsEntities();
        Assert.assertEquals(0, splitSnapshot.second.size());
        Assert.assertEquals(-1, splitSnapshot.first.longValue());
    }

    private Map<String, SplitEntity> splitsEntityMap(List<SplitEntity> entities) {
        Map<String, SplitEntity> entityMap = new HashMap<>();
        for (SplitEntity entity : entities) {
            entityMap.put(entity.getName(), entity);
        }
        return entityMap;
    }

    private List<Split> createSplits(int from, int count) {
        List<Split> splits = new ArrayList<>();
        for(int i=from; i<count + from; i++) {
            Split split = newSplit("split_" + i);
            splits.add(split);
        }
        return splits;
    }

    private Split newSplit(String name) {
        Split split = new Split();
        split.name = name;
        split.status = Status.ACTIVE;
        return split;
    }
}
