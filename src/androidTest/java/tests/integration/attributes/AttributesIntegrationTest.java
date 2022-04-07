package tests.integration.attributes;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.attributes.AttributesEntity;
import io.split.android.client.utils.Json;

public class AttributesIntegrationTest {

    private Context mContext;
    private SplitRoomDatabase mRoomDb;
    private SplitFactory mSplitFactory;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();
    }

    @Test
    public void testPersistentAttributes() throws InterruptedException {
        insertSplitsFromFileIntoDB();
        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitClient client = getSplitClient(readyLatch, true, null);
        readyLatch.await(5, TimeUnit.SECONDS);

        // 1. Evaluate without attrs
        Assert.assertEquals("on", client.getTreatment("workm"));

        // 2. Add attr and evaluate
        client.setAttribute("num_value", 10);
        Assert.assertEquals("on_num_10", client.getTreatment("workm"));

        // 3. Set all and evaluate
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("num_value", 10);
        attributes.put("str_value", "yes");
        attributes.put("num_value_a", 20);
        attributes.put("str_value_a", "no");
        client.setAttributes(attributes);
        Assert.assertEquals("on_num_10", client.getTreatment("workm"));

        // 4. Remove attr and evaluate
        client.removeAttribute("num_value");
        Assert.assertEquals("on", client.getTreatment("workm"));

        // 5. Clear and evaluate
        client.clearAttributes();
        Assert.assertEquals("on", client.getTreatment("workm"));
    }

    @Test
    public void testPersistentAttributes2() throws InterruptedException {
        // 1. Verify client.getAll() fetches corresponding attrs
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Map<String, Object> map = new HashMap<>();
        map.put("num_value", 10);

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("num_value", 10);

        insertSplitsFromFileIntoDB();
        String userKey = IntegrationHelper.dummyUserKey().matchingKey();
        AttributesEntity attributesEntity = new AttributesEntity(
                userKey,
                Json.toJson(map),
                System.currentTimeMillis());

        mRoomDb.attributesDao().update(attributesEntity);
        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitClient client = getSplitClient(readyLatch, true, userKey);
        readyLatch.await(5, TimeUnit.SECONDS);

        Assert.assertEquals(expectedMap, client.getAllAttributes());

        // 2. Verify evaluation uses attribute
        Assert.assertEquals("on_num_10", client.getTreatment("workm"));

        // 3. Perform clear and verify there are no attributes on DB
        client.clearAttributes();

        countDownLatch.await(1, TimeUnit.SECONDS);

        Assert.assertNull(mRoomDb.attributesDao().getByUserKey(userKey));
    }

    @Test
    public void testPersistentAttributesWithMultiClient2() throws InterruptedException {
       CountDownLatch countDownLatch = new CountDownLatch(1);
        String matchingKey = IntegrationHelper.dummyApiKey();

        Map<String, Object> map = new HashMap<>();
        map.put("num_value", 10);
        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("num_value", 10);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("string_value", "yes");
        Map<String, Object> expectedMap2 = new HashMap<>();
        expectedMap2.put("string_value", "yes");

        insertSplitsFromFileIntoDB();
        AttributesEntity attributesEntity = new AttributesEntity(
                matchingKey,
                Json.toJson(map),
                System.currentTimeMillis());
        AttributesEntity attributesEntity2 = new AttributesEntity(
                "new_key",
                Json.toJson(map2),
                System.currentTimeMillis());

        mRoomDb.attributesDao().update(attributesEntity);
        mRoomDb.attributesDao().update(attributesEntity2);
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch readyLatch2 = new CountDownLatch(1);
        SplitClient client = getSplitClient(readyLatch, true, matchingKey);
        SplitClient client2 = getSplitClient(readyLatch2, true, "new_key");
        readyLatch.await(5, TimeUnit.SECONDS);
        readyLatch2.await(5, TimeUnit.SECONDS);

        // 1. Verify client.getAll() fetches corresponding attrs
        Assert.assertEquals(expectedMap, client.getAllAttributes());
        Assert.assertEquals(expectedMap2, client2.getAllAttributes());

        // 2. Clear second client's attributes and check DB entry has been cleared
        client2.clearAttributes();
        countDownLatch.await(1, TimeUnit.SECONDS); // waiting since DB operations are async
        Assert.assertNull(mRoomDb.attributesDao().getByUserKey("new_key"));

        // 3. Verify evaluation with first client uses attribute
        Assert.assertEquals("on_num_10", client.getTreatment("workm"));

        // 4. Perform clear and verify there are no attributes on DB
        client.clearAttributes();

        countDownLatch.await(1, TimeUnit.SECONDS);

        Assert.assertNull(mRoomDb.attributesDao().getByUserKey(matchingKey));
    }

    @Test
    public void testNonPersistentAttributes() throws InterruptedException {
        Map<String, Object> map = new HashMap<>();
        map.put("num_value", 10);
        map.put("str_value", "yes");
        insertSplitsFromFileIntoDB();
        AttributesEntity attributesEntity = new AttributesEntity(
                IntegrationHelper.dummyUserKey().matchingKey(),
                Json.toJson(map),
                System.currentTimeMillis());

        mRoomDb.attributesDao().update(attributesEntity);
        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitClient client = getSplitClient(readyLatch, false, null);
        readyLatch.await(5, TimeUnit.SECONDS);

        // 1. Verify client does not fetch attrs from DB
        Assert.assertEquals(0, client.getAllAttributes().size());

        // 2. Verify evaluation ignores attrs from DB
        Assert.assertEquals("on", client.getTreatment("workm"));

        // 3. Create new attrs with client and verify DB has not been updated
        client.setAttribute("newKey", "newValue");

        Assert.assertEquals("{\"str_value\":\"yes\",\"num_value\":10}", mRoomDb.attributesDao().getByUserKey(IntegrationHelper.dummyUserKey().matchingKey()).getAttributes());
    }

    @Test
    public void testNonPersistentAttributes2() throws InterruptedException {
        insertSplitsFromFileIntoDB();
        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitClient client = getSplitClient(readyLatch, false, null);
        readyLatch.await(5, TimeUnit.SECONDS);

        // 1. Set attrs in client and evaluate.
        setAttributesInClientAndEvaluate(client);

        // 2. Verify overridden values work correctly.
        Map<String, Object> values = new HashMap<>();
        values.put("num_value", 20);
        Assert.assertEquals("on", client.getTreatment("workm", values));

        // 3. Delete attributes and evaluate with one time attrs.
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("num_value", 10);
        client.clearAttributes();
        Assert.assertEquals("on_num_10", client.getTreatment("workm", newValues));
    }

    private void setAttributesInClientAndEvaluate(SplitClient client) {
        client.setAttribute("num_value", 10);
        Assert.assertEquals("on_num_10", client.getTreatment("workm"));
    }

    private SplitClient getSplitClient(CountDownLatch readyLatch, boolean persistenceEnabled, String matchingKey) {
        if (mSplitFactory == null) {
            SplitClientConfig config = new TestableSplitConfigBuilder()
                    .enableDebug()
                    .featuresRefreshRate(9999)
                    .segmentsRefreshRate(9999)
                    .impressionsRefreshRate(9999)
                    .readTimeout(3000)
                    .isPersistentAttributesStorageEnabled(persistenceEnabled)
                    .streamingEnabled(false)
                    .build();

            mSplitFactory = IntegrationHelper.buildFactory(
                    IntegrationHelper.dummyApiKey(), IntegrationHelper.dummyUserKey(),
                    config, mContext, null, mRoomDb);
        }

        SplitClient client = mSplitFactory.client(
                new Key((matchingKey == null) ? IntegrationHelper.dummyApiKey() : matchingKey));
        SplitEventTaskHelper readyFromCacheTask = new SplitEventTaskHelper(readyLatch);
        client.on(SplitEvent.SDK_READY_FROM_CACHE, readyFromCacheTask);

        return client;
    }

    private void insertSplitsFromFileIntoDB() {
        List<Split> splitListFromJson = getSplitListFromJson();
        List<SplitEntity> entities = splitListFromJson.stream()
                .filter(split -> split.name != null)
                .map(split -> {
                    SplitEntity result = new SplitEntity();
                    result.setName(split.name);
                    result.setBody(Json.toJson(split));

                    return result;
                }).collect(Collectors.toList());

        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.DATBASE_MIGRATION_STATUS, GeneralInfoEntity.DATBASE_MIGRATION_STATUS_DONE));
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 1));

        mRoomDb.splitDao().insert(entities);
    }

    private List<Split> getSplitListFromJson() {
        FileHelper fileHelper = new FileHelper();
        String s = fileHelper.loadFileContent(mContext, "attributes_test_split_change.json");

        SplitChange changes = Json.fromJson(s, SplitChange.class);

        return changes.splits;
    }
}
