package tests.integration.encryption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import fake.LifecycleManagerStub;
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.ImpressionsCountEntity;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.impressions.unique.UniqueKeyEntity;
import io.split.android.client.utils.Json;

public class EncryptionTest {

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private final LifecycleManagerStub mLifecycleManager = new LifecycleManagerStub();

    @Test
    public void splitsSegmentsEventsAreEncrypted() throws IOException, InterruptedException {
        SplitRoomDatabase testDatabase = DatabaseHelper.getTestDatabase(mContext);

        SplitFactory factory = createFactory(mContext, testDatabase, true, ImpressionsMode.DEBUG);
        CountDownLatch latch = new CountDownLatch(1);

        factory.client().on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                client.track("user", "some_event", 25);
                client.track("user", "some_event_2", 30);
                factory.destroy();
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        Thread.sleep(200);

        verifySplits(testDatabase);
        verifySegments(testDatabase);
        verifyEvents(testDatabase);
    }

    @Test
    public void impressionsAreEncrypted() throws IOException, InterruptedException {
        SplitRoomDatabase testDatabase = DatabaseHelper.getTestDatabase(mContext);

        SplitFactory factory = createFactory(mContext, testDatabase, true, ImpressionsMode.DEBUG);
        CountDownLatch latch = new CountDownLatch(1);

        factory.client().on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                client.getTreatment("FACUNDO_TEST");
                client.getTreatment("testing");
                factory.destroy();
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        Thread.sleep(200);

        verifyImpressions(testDatabase);
    }

    @Test
    public void impressionsCountAreEncrypted() throws IOException, InterruptedException {
        SplitRoomDatabase testDatabase = DatabaseHelper.getTestDatabase(mContext);

        SplitFactory factory = createFactory(mContext, testDatabase, true, ImpressionsMode.OPTIMIZED);
        CountDownLatch latch = new CountDownLatch(1);

        factory.client().on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                for (int i = 0; i < 10; i++) {
                    client.getTreatment("FACUNDO_TEST");
                    for (int j = 0; j < 2; j++) {
                        client.getTreatment("testing");
                    }
                }

                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        mLifecycleManager.simulateOnPause();
        Thread.sleep(200);
        mLifecycleManager.simulateOnResume();
        Thread.sleep(200);

        verifyImpressionsCount(testDatabase);
    }

    @Test
    public void uniqueKeysAreEncrypted() throws IOException, InterruptedException {
        SplitRoomDatabase testDatabase = DatabaseHelper.getTestDatabase(mContext);

        SplitFactory factory = createFactory(mContext, testDatabase, true, ImpressionsMode.NONE);
        CountDownLatch latch = new CountDownLatch(1);

        factory.client().on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                client.getTreatment("FACUNDO_TEST");
                client.getTreatment("testing");
                factory.destroy();
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        Thread.sleep(250);

        verifyUniqueKeys(testDatabase);
    }

    @Test
    public void migrationToEncryptedIsCorrectlyApplied() throws IOException, InterruptedException {
        SplitRoomDatabase testDatabase = DatabaseHelper.getTestDatabase(mContext);

        SplitFactory factory = createFactory(mContext, testDatabase, false, ImpressionsMode.DEBUG);
        CountDownLatch latch = new CountDownLatch(1);

        factory.client().on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                client.track("user", "some_event", 25);
                client.track("user", "some_event_2", 30);
                client.getTreatment("FACUNDO_TEST");
                client.getTreatment("testing");
                mLifecycleManager.simulateOnPause();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
                mLifecycleManager.simulateOnResume();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
                client.flush();
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        Thread.sleep(500);

        CountDownLatch reInitLatch = new CountDownLatch(1);
        SplitFactory reInitFactory = createFactory(mContext, testDatabase, true, ImpressionsMode.DEBUG);
        reInitFactory.client().on(SplitEvent.SDK_READY_FROM_CACHE, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                reInitLatch.countDown();
            }
        });

        assertTrue(reInitLatch.await(2, TimeUnit.SECONDS));
        verifySplits(testDatabase);
        verifySegments(testDatabase);
        verifyEvents(testDatabase);
    }

    private static void verifySplits(SplitRoomDatabase testDatabase) {
        List<SplitEntity> all = testDatabase.splitDao().getAll();
        for (SplitEntity splitEntity : all) {
            boolean nameCondition = splitEntity.getName().trim().endsWith("=");
            if (!nameCondition) {
                fail("Split name not encrypted, it was " + splitEntity.getName());
            }

            boolean bodyCondition = splitEntity.getBody().trim().endsWith("=");
            if (!bodyCondition) {
                fail("Body not encrypted, it was " + splitEntity.getBody());
            }
        }

        assertEquals(2, all.size());
    }

    private static void verifySegments(SplitRoomDatabase testDatabase) {
        List<MySegmentEntity> all = testDatabase.mySegmentDao().getAll();
        for (MySegmentEntity segmentEntity : all) {
            boolean nameCondition = segmentEntity.getUserKey().trim().endsWith("=");
            if (!nameCondition) {
                fail("Segment user key not encrypted, it was " + segmentEntity.getUserKey());
            }

            boolean bodyCondition = segmentEntity.getSegmentList().trim().endsWith("=");
            if (!bodyCondition) {
                fail("Segment list not encrypted, it was " + segmentEntity.getSegmentList());
            }
        }

        assertEquals(1, all.size());
    }

    private static void verifyEvents(SplitRoomDatabase testDatabase) {
        List<EventEntity> all = testDatabase.eventDao().getAll();
        for (EventEntity entity : all) {
            boolean bodyCondition = entity.getBody().trim().endsWith("=");
            if (!bodyCondition) {
                fail("Event body not encrypted, it was " + entity.getBody());
            }
        }

        assertEquals(2, all.size());
    }

    private static void verifyImpressions(SplitRoomDatabase testDatabase) {
        List<ImpressionEntity> all = testDatabase.impressionDao().getAll();
        for (ImpressionEntity entity : all) {
            boolean bodyCondition = entity.getBody().trim().endsWith("=");
            if (!bodyCondition) {
                fail("Impression body not encrypted, it was " + entity.getBody());
            }

            boolean nameCondition = entity.getTestName().trim().endsWith("=");
            if (!nameCondition) {
                fail("Name not encrypted, it was " + entity.getTestName());
            }
        }

        assertEquals(2, all.size());
    }

    private static void verifyImpressionsCount(SplitRoomDatabase testDatabase) {
        List<ImpressionsCountEntity> all = testDatabase.impressionsCountDao().getAll();
        for (ImpressionsCountEntity entity : all) {
            boolean bodyCondition = entity.getBody().endsWith("}");
            if (bodyCondition) {
                fail("Impression count body not encrypted, it was " + entity.getBody());
            }
        }

        assertEquals(2, all.size());
    }

    private static void verifyUniqueKeys(SplitRoomDatabase testDatabase) {
        List<UniqueKeyEntity> all = testDatabase.uniqueKeysDao().getAll();
        for (UniqueKeyEntity entity : all) {
            boolean listCondition = entity.getFeatureList().trim().endsWith("=");
            if (!listCondition) {
                fail("Unique key body not encrypted, it was " + entity.getFeatureList());
            }

            boolean keyCondition = entity.getUserKey().trim().endsWith("=");
            if (!keyCondition) {
                fail("Unique key body not encrypted, it was " + entity.getUserKey());
            }
        }

        assertEquals(1, all.size());
    }

    private SplitFactory createFactory(
            Context mContext,
            SplitRoomDatabase splitRoomDatabase,
            boolean encryptionEnabled,
            ImpressionsMode impressionsMode) throws IOException {
        SplitClientConfig config = new TestableSplitConfigBuilder().ready(30000)
                .trafficType("client")
                .impressionsMode(impressionsMode)
                .impressionsRefreshRate(1000)
                .impressionsCountersRefreshRate(1000)
                .streamingEnabled(false)
                .eventFlushInterval(1000)
                .encryptionEnabled(encryptionEnabled)
                .build();

        HttpResponseMockDispatcher dispatcher = IntegrationHelper.buildDispatcher(getResponses());

        return IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                config,
                mContext,
                new HttpClientMock(dispatcher),
                splitRoomDatabase, null, null,
                mLifecycleManager);
    }

    private Map<String, IntegrationHelper.ResponseClosure> getResponses() {
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put("splitChanges", (uri, httpMethod, body) -> new HttpResponseMock(200, loadSplitChanges()));
        responses.put("mySegments/CUSTOMER_ID", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.dummyMySegments()));
        responses.put("events/bulk", (uri, httpMethod, body) -> new HttpResponseMock(404, ""));
        responses.put("testImpressions/bulk", (uri, httpMethod, body) -> new HttpResponseMock(404, ""));
        return responses;
    }

    private String loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        String change = fileHelper.loadFileContent(mContext, "split_changes_1.json");
        SplitChange parsedChange = Json.fromJson(change, SplitChange.class);
        parsedChange.splits = parsedChange.splits.stream().filter(s -> s.name.equals("FACUNDO_TEST") || s.name.equals("testing")).collect(Collectors.toList());
        parsedChange.since = parsedChange.till;

        return Json.toJson(parsedChange);
    }
}
