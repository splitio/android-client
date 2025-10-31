package io.split.android.client.service.impressions.observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static helper.IntegrationHelper.getSinceFromUri;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import fake.LifecycleManagerStub;
import fake.SynchronizerSpyImpl;
import helper.DatabaseHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.service.synchronizer.SynchronizerSpy;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheEntity;
import io.split.android.client.utils.Json;
import tests.integration.shared.TestingHelper;

public class DedupeIntegrationTest {

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private AtomicInteger mImpressionsListenerCount;
    private HttpClientMock mHttpClient;
    private SplitRoomDatabase mDatabase;
    private LifecycleManagerStub mLifecycleManager;
    private SynchronizerSpy mSynchronizerSpy;

    @Before
    public void setUp() throws IOException {
        mImpressionsListenerCount = new AtomicInteger(0);
        mDatabase = DatabaseHelper.getTestDatabase(mContext);
        mDatabase.clearAllTables();
        mHttpClient = new HttpClientMock(getDispatcher());
        mLifecycleManager = new LifecycleManagerStub();
        mSynchronizerSpy = new SynchronizerSpyImpl();
        mLifecycleManager.register(mSynchronizerSpy);
    }

    @Test
    public void impressionsAreDeduped() throws InterruptedException {
        SplitClient client = initSplitFactory(new TestableSplitConfigBuilder()
                .impressionsMode(ImpressionsMode.OPTIMIZED)
                .observerCacheExpirationPeriod(1000)
                .enableDebug()
                .impressionListener(new ImpressionListener() {
                    @Override
                    public void log(Impression impression) {
                        mImpressionsListenerCount.incrementAndGet();
                    }

                    @Override
                    public void close() {

                    }
                }), mHttpClient).client();

        for (int i = 0; i < 5; i++) {
            client.getTreatment("FACUNDO_TEST");
        }
        Thread.sleep(200);

        List<ImpressionEntity> all = mDatabase.impressionDao().getAll();
        assertEquals(1, all.size());
        assertEquals(5, mImpressionsListenerCount.get());
    }

    @Test
    public void impressionsAreDedupedWhenRecreatingInstance() throws InterruptedException {
        // create factory
        SplitFactory splitFactory = initSplitFactory(new TestableSplitConfigBuilder()
                .impressionsMode(ImpressionsMode.OPTIMIZED)
                .enableDebug()
                .impressionListener(new ImpressionListener() {
                    @Override
                    public void log(Impression impression) {
                        mImpressionsListenerCount.incrementAndGet();
                    }

                    @Override
                    public void close() {

                    }
                }), mHttpClient);

        // get the ready clients
        SplitClient client = splitFactory.client();
        SplitClient client2 = splitFactory.client("key2");
        CountDownLatch client2Latch = new CountDownLatch(1);
        client2.on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(client2Latch));
        if (!client2Latch.await(10, TimeUnit.SECONDS)) {
            fail("Client 2 is not ready");
        }

        // perform evaluations with both clients
        for (int i = 0; i < 5; i++) {
            client.getTreatment("FACUNDO_TEST");
            client2.getTreatment("FACUNDO_TEST");
        }

        Thread.sleep(200);

        // fetch impressions from database and destroy factory
        List<ImpressionEntity> all = mDatabase.impressionDao().getAll();
        splitFactory.destroy();
        Thread.sleep(200);

        // recreate factory
        splitFactory = initSplitFactory(new TestableSplitConfigBuilder()
                .impressionsMode(ImpressionsMode.OPTIMIZED)
                .enableDebug()
                .impressionListener(new ImpressionListener() {
                    @Override
                    public void log(Impression impression) {
                        mImpressionsListenerCount.incrementAndGet();
                    }

                    @Override
                    public void close() {

                    }
                }), mHttpClient);

        // get ready clients again
        client = splitFactory.client();
        client2 = splitFactory.client("key2");
        client2Latch = new CountDownLatch(1);
        client2.on(SplitEvent.SDK_READY_FROM_CACHE, new TestingHelper.TestEventTask(client2Latch));
        if (!client2Latch.await(10, TimeUnit.SECONDS)) {
            fail("Client 2 is not ready");
        }

        // perform same evaluations
        for (int i = 0; i < 5; i++) {
            client.getTreatment("FACUNDO_TEST");
            client2.getTreatment("FACUNDO_TEST");
        }
        Thread.sleep(200);

        // verify impressions from impression listener are ok
        assertEquals(20, mImpressionsListenerCount.get());

        // verify impressions in DB are only 2
        assertEquals(2, all.size());
    }

    @Test
    public void impressionsGeneratedInDebugModeHavePreviousTime() throws InterruptedException {
        // initialize SDK
        SplitClient client = initSplitFactory(new TestableSplitConfigBuilder()
                .impressionsMode(ImpressionsMode.DEBUG)
                .enableDebug()
                .impressionListener(new ImpressionListener() {
                    @Override
                    public void log(Impression impression) {
                        mImpressionsListenerCount.incrementAndGet();
                    }

                    @Override
                    public void close() {

                    }
                }), mHttpClient).client();

        // perform evaluations
        for (int i = 0; i < 2; i++) {
            client.getTreatment("FACUNDO_TEST");
        }
        Thread.sleep(200);

        // verify impressions in DB are only 2
        List<ImpressionEntity> all = mDatabase.impressionDao().getAll();
        assertEquals(2, all.size());

        // verify first impression has no previous time and second has
        Long firstPreviousTime = Json.fromJson(all.get(0).getBody(), KeyImpression.class).previousTime;
        Long secondPreviousTime = Json.fromJson(all.get(1).getBody(), KeyImpression.class).previousTime;
        assertTrue((firstPreviousTime == null && secondPreviousTime != null) || (firstPreviousTime != null && secondPreviousTime == null));

        // verify number of impressions through impressions listener
        assertEquals(2, mImpressionsListenerCount.get());
    }

    @Test
    public void expiredObserverCacheValuesExistingInDatabaseAreRemovedOnStartup() throws InterruptedException {
        // prepopulate DB with 2000 entries
        List<ImpressionsObserverCacheEntity> entities = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            entities.add(new ImpressionsObserverCacheEntity(i, i, System.currentTimeMillis()));
        }
        mDatabase.impressionsObserverCacheDao().insert(entities);

        // wait for them to expire
        Thread.sleep(2000);

        // initialize SDK
        SplitClient client = initSplitFactory(new TestableSplitConfigBuilder()
                .impressionsMode(ImpressionsMode.DEBUG)
                .streamingEnabled(false)
                .enableDebug()
                .impressionsDedupeTimeInterval(1)
                .observerCacheExpirationPeriod(100), mHttpClient).client();
        Thread.sleep(200);

        client.getTreatment("FACUNDO_TEST");
        Thread.sleep(100);
        mLifecycleManager.simulateOnPause();
        Thread.sleep(500);
        mLifecycleManager.simulateOnResume();

        while (mDatabase.impressionsObserverCacheDao().getAll(5).size() > 1) {
            Thread.sleep(100);
        }
        Thread.sleep(1000);

        int count = mDatabase.impressionsObserverCacheDao().getAll(3000).size();
        assertEquals(1, count);
    }

    @Test
    public void customImpressionsListenerIsExecutedInTheSameThreadThatFactoryIsInstantiated() throws InterruptedException {
        long threadId = Thread.currentThread().getId();
        final AtomicLong listenerThreadId = new AtomicLong();
        SplitClient client = initSplitFactory(new TestableSplitConfigBuilder()
                .impressionsMode(ImpressionsMode.DEBUG)
                .enableDebug()
                .impressionListener(new ImpressionListener() {
                    @Override
                    public void log(Impression impression) {
                        mImpressionsListenerCount.incrementAndGet();
                        listenerThreadId.set(Thread.currentThread().getId());
                    }

                    @Override
                    public void close() {

                    }
                }), mHttpClient).client();

        client.getTreatment("FACUNDO_TEST");

        assertEquals(threadId, listenerThreadId.get());
    }

    @Test
    public void impressionsAreDedupedDuringDedupeInterval() throws InterruptedException {
        SplitClient client = initSplitFactory(new TestableSplitConfigBuilder()
                .impressionsMode(ImpressionsMode.OPTIMIZED)
                .impressionsDedupeTimeInterval(500)
                .enableDebug()
                .impressionListener(new ImpressionListener() {
                    @Override
                    public void log(Impression impression) {
                        mImpressionsListenerCount.incrementAndGet();
                    }

                    @Override
                    public void close() {

                    }
                }), mHttpClient).client();

        for (int i = 0; i < 5; i++) {
            client.getTreatment("FACUNDO_TEST");
        }
        Thread.sleep(600); // delay for dedupe interval
        for (int i = 0; i < 5; i++) {
            client.getTreatment("FACUNDO_TEST");
        }
        Thread.sleep(200); // delay for persistence

        assertEquals(2, mDatabase.impressionDao().getAll().size()); // impressions after the interval passes are not deduped
        assertEquals(10, mImpressionsListenerCount.get()); // all impressions are received by listener
    }

    private HttpResponseMockDispatcher getDispatcher() {
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put("splitChanges", (uri, httpMethod, body) -> {
            String since = getSinceFromUri(uri);
            if (since.equals("-1")) {
                return new HttpResponseMock(200, loadSplitChanges());
            } else {
                return new HttpResponseMock(200, IntegrationHelper.emptySplitChanges(1602796638344L, 1602796638344L));
            }
        });

        responses.put(IntegrationHelper.ServicePath.MEMBERSHIPS + "/" + "/CUSTOMER_ID", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));
        responses.put(IntegrationHelper.ServicePath.MEMBERSHIPS + "/" + "/key2", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));

        return IntegrationHelper.buildDispatcher(responses);
    }

    private SplitFactory initSplitFactory(TestableSplitConfigBuilder builder, HttpClientMock httpClient) throws InterruptedException {
        CountDownLatch innerLatch = new CountDownLatch(1);
        SplitFactory factory = IntegrationHelper.buildFactory(
                "sdk_key_1",
                IntegrationHelper.dummyUserKey(),
                builder.build(),
                mContext,
                httpClient,
                mDatabase, mSynchronizerSpy, null,
                mLifecycleManager);

        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(innerLatch));
        boolean await = innerLatch.await(5, TimeUnit.SECONDS);
        if (!await) {
            fail("Client is not ready");
        }
        return factory;
    }

    private String loadSplitChanges() {
        return IntegrationHelper.loadSplitChanges(mContext, "split_changes_1.json");
    }
}
