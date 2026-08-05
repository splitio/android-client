package tests.integration.attributes;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.reflect.TypeToken;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.attributes.AttributesEntity;
import io.split.android.client.utils.Json;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;


public class AttributesLoadPersistRaceInstrumentedTest {

    /**
     * Persist/clear tasks are scheduled with a 5s delay, so allow comfortably more than that.
     */
    private static final int AWAIT_TIMEOUT_SECONDS = 10;
    private static final long POLL_INTERVAL_MS = 200L;

    private Context mContext;
    private SplitRoomDatabase mRoomDb;
    private SplitFactory mSplitFactory;
    private MockWebServer mWebServer;

    @Before
    public void setup() {
        setupServer();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();
    }

    @After
    public void tearDown() throws IOException {
        mWebServer.shutdown();
    }

    /**
     * With encryption enabled, set a real encrypted attribute via client.setAttribute(...),
     * confirm a row exists, then clearAttributes() and confirm the real cipher-encrypted row is deleted.
     */
    @Test
    public void encryptedClearActuallyDeletesPersistedRow() throws InterruptedException {
        insertSplitsFromFileIntoDB();

        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitClient client = getSplitClient(readyLatch, true, true, null);
        Assert.assertTrue(readyLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        client.setAttribute("num_value", 10);

        waitForPersistedRowCount(1);

        List<AttributesEntity> allBeforeClear = mRoomDb.attributesDao().getAll();
        Assert.assertEquals(1, allBeforeClear.size());
        // The persisted key must be encrypted - it must not equal the plaintext matching key.
        Assert.assertNotEquals(IntegrationHelper.dummyUserKey().matchingKey(), allBeforeClear.get(0).getUserKey());

        client.clearAttributes();

        waitForPersistedRowCount(0);

        Assert.assertTrue(mRoomDb.attributesDao().getAll().isEmpty());
    }

    /**
     * A stale attribute value is persisted in the DB before the client is created.
     * Immediately after obtaining the client reference, a new value is set
     * for the same key.
     * <p>
     * Once SDK_READY_FROM_CACHE fires, the newer in-memory value must win. The
     * async load must not resurrect the stale persisted value over it.
     */
    @Test
    public void loadDoesNotResurrectNewerValueSetRightAfterClientCreation() throws InterruptedException {
        insertSplitsFromFileIntoDB();

        String userKey = IntegrationHelper.dummyUserKey().matchingKey();
        Map<String, Object> staleMap = new HashMap<>();
        staleMap.put("num_value", 10);
        mRoomDb.attributesDao().update(new AttributesEntity(
                userKey,
                Json.toJson(staleMap),
                System.currentTimeMillis()));

        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitClient client = getSplitClient(readyLatch, true, false, userKey);

        // Race: set a different value for the same key immediately after obtaining the client,
        // before/while the async load-from-persistence task (triggered internally on
        // ATTRIBUTES_LOADED_FROM_STORAGE) may be running.
        client.setAttribute("num_value", 99);

        Assert.assertTrue(readyLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        Assert.assertEquals(99, client.getAttribute("num_value"));
    }

    /**
     * DB is prefilled with {A: old, SIBLING: value}.
     * <p>
     * After the client is ready, A is updated to a new value, which schedules a
     * delayed (5s) persist task that reads AttributesStorage.getAll() live at execute time.
     * <p>
     * Once that task actually runs, the persisted row must contain BOTH the updated A and the untouched
     * SIBLING key.
     */
    @Test
    public void siblingKeySurvivesDelayedPersistAfterMerge() throws InterruptedException {
        insertSplitsFromFileIntoDB();

        String userKey = IntegrationHelper.dummyUserKey().matchingKey();
        SplitCipher cipher = encryptionCipher();
        String encryptedUserKey = cipher.encrypt(userKey);

        Map<String, Object> seedMap = new HashMap<>();
        seedMap.put("A", "old");
        seedMap.put("SIBLING", "value");
        mRoomDb.attributesDao().update(new AttributesEntity(
                encryptedUserKey,
                cipher.encrypt(Json.toJson(seedMap)),
                System.currentTimeMillis()));

        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitClient client = getSplitClient(readyLatch, true, true, userKey);
        Assert.assertTrue(readyLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        client.setAttribute("A", "new");

        // Wait for the delayed persist task to run and write the updated value to SQLite.
        Map<String, Object> persisted = awaitValue(() -> {
            AttributesEntity entity = mRoomDb.attributesDao().getByUserKey(encryptedUserKey);
            if (entity == null) {
                return null;
            }
            Map<String, Object> decoded = decryptAttributes(cipher, entity);

            return "new".equals(decoded.get("A")) ? decoded : null;
        }, AWAIT_TIMEOUT_SECONDS);

        Assert.assertNotNull("Expected the delayed persist task to write A=new for the encrypted user key", persisted);
        Assert.assertEquals("new", persisted.get("A"));
        Assert.assertEquals("value", persisted.get("SIBLING"));
    }

    /**
     * Polls until {@code condition} yields a non-null value or the timeout elapses. Exceptions from
     * {@code condition} are treated as "not ready yet", since the row may be mid-write.
     */
    @Nullable
    private static <T> T awaitValue(Callable<T> condition, int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        while (System.currentTimeMillis() < deadline) {
            try {
                T value = condition.call();
                if (value != null) {
                    return value;
                }
            } catch (Exception ignored) {
                // Retry on the next iteration.
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }

        return null;
    }

    private void waitForPersistedRowCount(int expectedCount) throws InterruptedException {
        awaitValue(() -> mRoomDb.attributesDao().getAll().size() == expectedCount ? true : null, AWAIT_TIMEOUT_SECONDS);
    }

    private static SplitCipher encryptionCipher() {
        return SplitCipherFactory.create(IntegrationHelper.dummyApiKey(), true);
    }

    private static Map<String, Object> decryptAttributes(SplitCipher cipher, AttributesEntity entity) {
        return Json.genericValueMapFromJson(
                cipher.decrypt(entity.getAttributes()),
                new TypeToken<Map<String, Object>>() {
                }.getType());
    }

    private SplitClient getSplitClient(CountDownLatch readyLatch, boolean persistenceEnabled, boolean encryptionEnabled, String matchingKey) {
        if (mSplitFactory == null) {
            final String url = mWebServer.url("/").url().toString();
            ServiceEndpoints endpoints = ServiceEndpoints.builder()
                    .apiEndpoint(url).eventsEndpoint(url).build();
            SplitClientConfig config = new TestableSplitConfigBuilder()
                    .enableDebug()
                    .serviceEndpoints(endpoints)
                    .featuresRefreshRate(9999)
                    .segmentsRefreshRate(9999)
                    .impressionsRefreshRate(9999)
                    .readTimeout(3000)
                    .isPersistentAttributesStorageEnabled(persistenceEnabled)
                    .encryptionEnabled(encryptionEnabled)
                    .streamingEnabled(false)
                    .build();

            mSplitFactory = IntegrationHelper.buildFactory(
                    IntegrationHelper.dummyApiKey(), IntegrationHelper.dummyUserKey(),
                    config, mContext, null, mRoomDb);
        }

        SplitClient client = mSplitFactory.client(
                new Key((matchingKey == null) ? IntegrationHelper.dummyUserKey().matchingKey() : matchingKey));
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

        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 1));

        mRoomDb.splitDao().insert(entities);
    }

    private List<Split> getSplitListFromJson() {
        FileHelper fileHelper = new FileHelper();
        String s = fileHelper.loadFileContent(mContext, "attributes_test_split_change.json");

        SplitChange changes = IntegrationHelper.getChangeFromJsonString(s);

        return changes.splits;
    }

    private void setupServer() {
        mWebServer = new MockWebServer();

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.dummyAllSegments());
                } else if (request.getPath().contains("/splitChanges")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody(IntegrationHelper.emptySplitChanges(-1, 10000));
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }
}
