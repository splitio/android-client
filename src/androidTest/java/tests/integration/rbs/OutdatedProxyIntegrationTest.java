package tests.integration.rbs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static helper.IntegrationHelper.emptyTargetingRulesChanges;
import static helper.IntegrationHelper.getSinceFromUri;
import static helper.IntegrationHelper.getSpecFromUri;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import helper.DatabaseHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.TestingConfig;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import tests.integration.shared.TestingHelper;

public class OutdatedProxyIntegrationTest {

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();

    private MockWebServer mWebServer;
    private Map<String, AtomicInteger> mEndpointHits;
    private Map<String, CountDownLatch> mLatches;
    private final AtomicBoolean mOutdatedProxy = new AtomicBoolean(false);
    private final AtomicBoolean mSimulatedProxyError = new AtomicBoolean(false);
    private final AtomicBoolean mRecoveryHit = new AtomicBoolean(false);

    @Before
    public void setUp() throws IOException {
        mEndpointHits = new ConcurrentHashMap<>();
        mOutdatedProxy.set(false);
        initializeLatches();

        mWebServer = new MockWebServer();
        mWebServer.setDispatcher(new Dispatcher() {
            @NonNull
            @Override
            public MockResponse dispatch(@NonNull RecordedRequest request) {
                if (request.getRequestUrl().encodedPathSegments().contains(IntegrationHelper.ServicePath.SPLIT_CHANGES)) {
                    updateEndpointHit(IntegrationHelper.ServicePath.SPLIT_CHANGES);
                    float specFromUri = Float.parseFloat(getSpecFromUri(request.getRequestUrl().uri()));
                    if (mOutdatedProxy.get() && specFromUri > 1.2f) {
                        mSimulatedProxyError.set(true);
                        return new MockResponse().setResponseCode(400);
                    } else if (mOutdatedProxy.get()) {
                            String body = (getSinceFromUri(request.getRequestUrl().uri()).equals("-1")) ?
                                    IntegrationHelper.loadLegacySplitChanges(mContext, "split_changes_legacy.json") :
                                    emptyTargetingRulesChanges(1506703262916L, -1L);
                        return new MockResponse().setResponseCode(200)
                                .setBody(body);
                    }

                    if (!mOutdatedProxy.get()) {
                        if (request.getRequestUrl().uri().toString().contains("?s=1.3&since=-1&rbSince=-1")) {
                            mRecoveryHit.set(true);
                        }
                    }

                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.loadSplitChanges(mContext, "split_changes_rbs.json"));
                } else if (request.getRequestUrl().encodedPathSegments().contains(IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    updateEndpointHit(IntegrationHelper.ServicePath.MEMBERSHIPS);

                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.emptyAllSegments());
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        });
        mWebServer.start();
    }

    @After
    public void tearDown() throws IOException {
        mWebServer.shutdown();
    }

    @Test
    public void clientIsReadyEvenWhenUsingOutdatedProxy() {
        mOutdatedProxy.set(true);
        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), getFactory());

        assertNotNull(readyClient);
        assertFalse(mRecoveryHit.get());
        assertTrue(mSimulatedProxyError.get());
    }

    @Test
    public void clientIsReadyWithLatestProxy() {
        mOutdatedProxy.set(false);
        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), getFactory());

        assertNotNull(readyClient);
        assertFalse(mRecoveryHit.get() && mOutdatedProxy.get());
        assertFalse(mSimulatedProxyError.get());
    }

    @Test
    public void clientRecoversFromOutdatedProxy() {
        mOutdatedProxy.set(false);
        SplitRoomDatabase database = DatabaseHelper.getTestDatabase(mContext);
        database.generalInfoDao().update(new GeneralInfoEntity("lastProxyCheckTimestamp", System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(62)));
        SplitClient readyClient = getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), getFactory(database));

        assertNotNull(readyClient);
        assertTrue(mRecoveryHit.get() && !mOutdatedProxy.get());
        assertFalse(mSimulatedProxyError.get());
    }

    private void initializeLatches() {
        mLatches = new ConcurrentHashMap<>();
        mLatches.put(IntegrationHelper.ServicePath.SPLIT_CHANGES, new CountDownLatch(1));
        mLatches.put(IntegrationHelper.ServicePath.MEMBERSHIPS, new CountDownLatch(1));
    }

    private void updateEndpointHit(String splitChanges) {
        if (mEndpointHits.containsKey(splitChanges)) {
            mEndpointHits.get(splitChanges).getAndIncrement();
        } else {
            mEndpointHits.put(splitChanges, new AtomicInteger(1));
        }

        if (mLatches.containsKey(splitChanges)) {
            mLatches.get(splitChanges).countDown();
        }
    }

    protected SplitFactory getFactory() {
        return getFactory(null);
    }

    protected SplitFactory getFactory(SplitRoomDatabase database) {
        TestableSplitConfigBuilder configBuilder = new TestableSplitConfigBuilder()
                .enableDebug()
                .serviceEndpoints(ServiceEndpoints.builder()
                        .apiEndpoint("http://" + mWebServer.getHostName() + ":" + mWebServer.getPort())
                        .build());

            configBuilder.streamingEnabled(false);
        configBuilder.ready(10000);
        TestingConfig testingConfig = new TestingConfig();
        testingConfig.setFlagsSpec("1.3");
        return IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                configBuilder.build(),
                mContext,
                null, database, null, testingConfig, null);
    }

    protected SplitClient getReadyClient(String matchingKey, SplitFactory factory) {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        SplitClient client = factory.client(matchingKey);
        boolean await;
        client.on(SplitEvent.SDK_READY, TestingHelper.testTask(countDownLatch));
        try {
            await = countDownLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!await) {
            fail("Client is not ready");
        }

        return client;
    }
}
