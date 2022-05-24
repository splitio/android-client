package tests.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import helper.DatabaseHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import helper.TestingHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.impressions.unique.UniqueKeyEntity;
import io.split.android.client.utils.Logger;

public class UniqueKeysIntegrationTest {

    private SplitFactory mSplitFactory;
    private final AtomicReference<String> mUniqueKeysBody = new AtomicReference<>("");
    private final CountDownLatch mMtkLatch = new CountDownLatch(1);
    private final AtomicInteger mMtkEndpointHitCount = new AtomicInteger(0);
    private SplitRoomDatabase mDatabase;
    private Context mContext;
    private HttpClientMock mHttpClient;

    @Before
    public void setUp() throws IOException {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mHttpClient = new HttpClientMock(IntegrationHelper.buildDispatcher(getMockResponses()));
        mDatabase = DatabaseHelper.getTestDatabase(mContext);
        mSplitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                new Key("key1"),
                new TestableSplitConfigBuilder()
                        .ready(30000)
                        .streamingEnabled(true)
                        .enableDebug()
                        .trafficType("account")
                        .build(),
                mContext,
                mHttpClient,
                mDatabase
        );

        mDatabase.clearAllTables();
        mMtkEndpointHitCount.set(0);
        mUniqueKeysBody.set("");
    }

    @Test
    public void verifyRequestBody() throws InterruptedException {
        SplitClient client = mSplitFactory.client();
        SplitClient client2 = mSplitFactory.client("key2");

        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch readyLatch2 = new CountDownLatch(1);
        TestingHelper.TestEventTask task = new TestingHelper.TestEventTask(readyLatch);
        TestingHelper.TestEventTask task2 = new TestingHelper.TestEventTask(readyLatch2);
        client.on(SplitEvent.SDK_READY, task);
        client2.on(SplitEvent.SDK_READY, task2);
        readyLatch.await(5, TimeUnit.SECONDS);
        readyLatch2.await(5, TimeUnit.SECONDS);

        for (int i = 0; i < 2; i++) {
            client.getTreatment("android_test_2");
            client.getTreatment("android_test_3");
        }

        client2.getTreatment("android_test_2");

        mMtkEndpointHitCount.set(2);
        client.flush();
        boolean await = mMtkLatch.await(10, TimeUnit.SECONDS);

        assertTrue(await);
        assertEquals("{\"keys\":[{\"fs\":[\"android_test_2\",\"android_test_3\"],\"k\":\"key1\"},{\"fs\":[\"android_test_2\"],\"k\":\"key2\"}]}", mUniqueKeysBody.get());

        client.destroy();
        client2.destroy();
    }

    @Test
    public void verifyKeyFeaturesAreMerged() throws InterruptedException {
        SplitClient client = mSplitFactory.client();

        CountDownLatch readyLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask task = new TestingHelper.TestEventTask(readyLatch);
        client.on(SplitEvent.SDK_READY, task);
        readyLatch.await(5, TimeUnit.SECONDS);

        mDatabase.uniqueKeysDao().insert(new UniqueKeyEntity("key1", "[\"f1\",\"f2\"]", System.currentTimeMillis(), 0));
        mDatabase.uniqueKeysDao().insert(new UniqueKeyEntity("key1", "[\"f2\",\"f3\",\"f4\"]", System.currentTimeMillis(), 0));
        mDatabase.uniqueKeysDao().insert(new UniqueKeyEntity("key2", "[\"f1\"]", System.currentTimeMillis(), 0));

        mMtkEndpointHitCount.set(2);
        client.flush();
        boolean await = mMtkLatch.await(10, TimeUnit.SECONDS);

        assertTrue(await);
        assertEquals("{\"keys\":[{\"fs\":[\"f2\",\"f3\",\"f4\",\"f1\"],\"k\":\"key1\"},{\"fs\":[\"f1\"],\"k\":\"key2\"}]}", mUniqueKeysBody.get());

        client.destroy();
    }

    @Test
    public void retryIsPerformed() throws InterruptedException {
        SplitClient client = mSplitFactory.client();

        CountDownLatch readyLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask task = new TestingHelper.TestEventTask(readyLatch);
        client.on(SplitEvent.SDK_READY, task);
        readyLatch.await(5, TimeUnit.SECONDS);

        for (int i = 0; i < 8; i++) {
            client.getTreatment("android_test_2");
        }

        client.flush();
        boolean await = mMtkLatch.await(50, TimeUnit.SECONDS);

        assertTrue(await);
        assertEquals(3, mMtkEndpointHitCount.get());

        client.destroy();
    }

    @Test
    public void verifyDatabaseIsEmptyAfterRequest() throws InterruptedException {
        SplitClient client = mSplitFactory.client();

        CountDownLatch readyLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask task = new TestingHelper.TestEventTask(readyLatch);
        client.on(SplitEvent.SDK_READY, task);
        readyLatch.await(5, TimeUnit.SECONDS);

        for (int i = 0; i < 2; i++) {
            client.getTreatment("android_test_2");
            client.getTreatment("android_test_3");
        }

        mMtkEndpointHitCount.set(2);
        client.flush();
        boolean await = mMtkLatch.await(10, TimeUnit.SECONDS);

        assertTrue(await);
        assertEquals("{\"keys\":[{\"fs\":[\"android_test_2\",\"android_test_3\"],\"k\":\"key1\"}]}", mUniqueKeysBody.get());
        Thread.sleep(500);
        assertEquals(0, mDatabase.uniqueKeysDao().getAll().size());

        client.destroy();
    }

    @Test
    public void verifyNoHitsInMtkEndpoints() throws InterruptedException {
        SplitClient client = mSplitFactory.client();

        CountDownLatch readyLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask task = new TestingHelper.TestEventTask(readyLatch);
        client.on(SplitEvent.SDK_READY, task);
        readyLatch.await(5, TimeUnit.SECONDS);
        boolean await = mMtkLatch.await(3, TimeUnit.SECONDS);

        assertFalse(await);
        assertEquals("", mUniqueKeysBody.get());
        assertEquals(0, mMtkEndpointHitCount.get());

        client.destroy();
    }

    @Test
    public void verifyFeatureDisabled() throws InterruptedException {
        mSplitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                new Key("key1"),
                new SplitClientConfig.Builder()
                        .ready(30000)
                        .streamingEnabled(true)
                        .enableDebug()
                        .trafficType("account")
                        .build(),
                mContext,
                mHttpClient,
                mDatabase
        );

        SplitClient client = mSplitFactory.client();

        CountDownLatch readyLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask task = new TestingHelper.TestEventTask(readyLatch);
        client.on(SplitEvent.SDK_READY, task);
        readyLatch.await(5, TimeUnit.SECONDS);

        client.getTreatment("android_test_2");
        client.getTreatment("android_test_3");
        Thread.sleep(500);

        assertEquals(0, mDatabase.uniqueKeysDao().getAll().size());

        boolean await = mMtkLatch.await(3, TimeUnit.SECONDS);

        assertFalse(await);
        assertEquals("", mUniqueKeysBody.get());
        assertEquals(0, mMtkEndpointHitCount.get());

        client.destroy();
    }

    private Map<String, IntegrationHelper.ResponseClosure> getMockResponses() {
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();

        responses.put("v1/keys/cs", (uri, httpMethod, body) -> {
            Logger.d("counter " + mMtkEndpointHitCount.get());
            if (mMtkEndpointHitCount.incrementAndGet() == 3) {
                mUniqueKeysBody.set(body);
                mMtkLatch.countDown();

                return new HttpResponseMock(200, "{}");
            } else {
                return new HttpResponseMock(500, "{}");
            }
        });

        responses.put("splitChanges", (uri, httpMethod, body) -> {
            String splitChange = "{\"splits\":[{\"trafficTypeName\":\"account\",\"name\":\"android_test_2\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-1955610140,\"seed\":-633015570,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1648733409158,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"account\",\"attribute\":null},\"matcherType\":\"IN_SPLIT_TREATMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":{\"split\":\"android_test_3\",\"treatments\":[\"on\"]},\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0}],\"label\":\"in split android_test_3 treatment [on]\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"account\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":100}],\"label\":\"default rule\"}]},{\"trafficTypeName\":\"account\",\"name\":\"android_test_3\",\"trafficAllocation\":100,\"trafficAllocationSeed\":-397942789,\"seed\":1852089605,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"off\",\"changeNumber\":1648733496087,\"algo\":2,\"configurations\":{},\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"account\",\"attribute\":null},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"android_test\"},\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0}],\"label\":\"in segment android_test\"}]}],\"since\":-1,\"till\":1648733409158}";

            return new HttpResponseMock(200, splitChange);
        });

        IntegrationHelper.ResponseClosure mySegmentsResponse = (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments());
        responses.put("mySegments/key1", mySegmentsResponse);
        responses.put("mySegments/key2", mySegmentsResponse);

        responses.put("v2/auth", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.streamingEnabledToken()));
        return responses;
    }
}
