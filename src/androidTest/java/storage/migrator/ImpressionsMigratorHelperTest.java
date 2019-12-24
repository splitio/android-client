package storage.migrator;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import helper.FileHelper;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.IImpressionsStorage;
import io.split.android.client.impressions.StoredImpressions;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.migrator.ImpressionsMigratorHelper;
import io.split.android.client.storage.legacy.ImpressionsFileStorage;
import io.split.android.client.storage.legacy.ImpressionsStorageManager;
import io.split.android.client.storage.legacy.ImpressionsStorageManagerConfig;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.TimeUtils;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ImpressionsMigratorHelperTest {
    FileHelper mFileHelper = new FileHelper();
    ImpressionsMigratorHelper mMigrator;
    ImpressionsStorageManager mLegacyImpressionsStorage;
    TimeUtils mTimeUtils = new TimeUtils();

    @Before
    public void setup() {
        File cacheFolder = InstrumentationRegistry.getInstrumentation().getContext().getCacheDir();
        File rootFolder = mFileHelper.emptyAndGetTestFolder(cacheFolder, "impressions_folder_test");

        IImpressionsStorage fileStorage = new ImpressionsFileStorage(rootFolder, "impressions");
        mLegacyImpressionsStorage = new ImpressionsStorageManager(fileStorage, new ImpressionsStorageManagerConfig());
        mMigrator = new ImpressionsMigratorHelper(mLegacyImpressionsStorage);
    }

    @Test
    public void basicMigration() throws IOException {

        List<KeyImpression> impressions = createImpressions(1, 50);
        mLegacyImpressionsStorage.storeImpressions(impressions);

        List<ImpressionEntity> entities = mMigrator.loadLegacyImpressionsAsEntities();
        Map<String, ImpressionEntity> entityMap = impressionsEntityMap(entities);

        ImpressionEntity entity1 = entityMap.get("impression_1");
        ImpressionEntity entity2 = entityMap.get("impression_25");
        ImpressionEntity entity3 = entityMap.get("impression_50");

        KeyImpression impression1 = Json.fromJson(entity1.getBody(), KeyImpression.class);
        KeyImpression impression2 = Json.fromJson(entity2.getBody(), KeyImpression.class);
        KeyImpression impression3 = Json.fromJson(entity3.getBody(), KeyImpression.class);

        Assert.assertEquals(50, entities.size());

        Assert.assertNotNull(entity1);
        Assert.assertNotNull(entity2);
        Assert.assertNotNull(entity3);

        Assert.assertEquals("impression_1", impression1.keyName);
        Assert.assertEquals("feature_2", impression1.feature);
        Assert.assertEquals(100L, (long)impression1.changeNumber);
        Assert.assertEquals("on", impression1.treatment);
        Assert.assertTrue(entity1.getCreatedAt() > 0);


        Assert.assertEquals("impression_25", impression2.keyName);
        Assert.assertEquals("feature_2", impression2.feature);
        Assert.assertEquals(100L, (long)impression2.changeNumber);
        Assert.assertEquals("on", impression2.treatment);
        Assert.assertTrue(entity2.getCreatedAt() > 0);


        Assert.assertEquals("impression_50", impression3.keyName);
        Assert.assertEquals("feature_1", impression3.feature);
        Assert.assertEquals(100L, (long)impression3.changeNumber);
        Assert.assertEquals("on", impression3.treatment);
        Assert.assertTrue(entity3.getCreatedAt() > 0);

    }

    @Test
    public void emptyMigration() {
        List<ImpressionEntity> entities = mMigrator.loadLegacyImpressionsAsEntities();
        Assert.assertEquals(0, entities.size());
    }

    private Map<String, ImpressionEntity> impressionsEntityMap(List<ImpressionEntity> entities) {
        Map<String, ImpressionEntity> entityMap = new HashMap<>();
        for (ImpressionEntity entity : entities) {
            KeyImpression impression = Json.fromJson(entity.getBody(), KeyImpression.class);
            entityMap.put(impression.keyName, entity);
        }
        return entityMap;
    }

    private List<KeyImpression> createImpressions(int from, int to) {
        List<KeyImpression> impressions = new ArrayList<>();
        for(int i=from; i<=to; i++) {
            KeyImpression impression = newImpression("feature_" + ((i % 2) == 0 ? 1 : 2), "impression_" + i);
            impressions.add(impression);
        }
        return impressions;
    }

    private KeyImpression newImpression(String feature, String key) {
        KeyImpression impression = new KeyImpression();
        impression.changeNumber = 100L;
        impression.feature = feature;
        impression.keyName = key;
        impression.treatment = "on";
        impression.time = mTimeUtils.timeInSeconds();
        return impression;
    }
}
