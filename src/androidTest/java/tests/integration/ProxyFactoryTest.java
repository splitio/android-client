package tests.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.SplitAuthenticatedRequest;
import io.split.android.client.network.SplitAuthenticator;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.service.synchronizer.ThreadUtils;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import tests.integration.shared.TestingHelper;

public class ProxyFactoryTest {

    private SplitFactory mSplitFactory;
    private final String mSplitChanges = new FileHelper().loadFileContent(
            InstrumentationRegistry.getInstrumentation().getContext(),
            "split_changes_1.json");

    @After
    public void tearDown() {
        if (mSplitFactory != null) {
            mSplitFactory.destroy();
        }
    }

    @Test
    public void initializeWithProxy() throws InterruptedException {
        final AtomicBoolean crashes = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);
        ThreadUtils.runInMainThread(() -> {
            try {
                mSplitFactory = SplitFactoryBuilder.build(IntegrationHelper.dummyApiKey(),
                        IntegrationHelper.dummyUserKey(),
                        new SplitClientConfig.Builder()
                                .proxyHost("https://custom_host.com")
                                .build(),
                        InstrumentationRegistry.getInstrumentation().getContext());
            } catch (Exception e) {
                crashes.set(true);
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertFalse(crashes.get());
    }

    @Test
    public void settingProxyReplacesAllUrls() throws IOException, InterruptedException {

        AtomicBoolean mySegmentsHit = new AtomicBoolean(false);
        AtomicBoolean splitChangesHit = new AtomicBoolean(false);
        CountDownLatch authLatch = new CountDownLatch(1);
        CountDownLatch eventsLatch = new CountDownLatch(1);

        // init server
        MockWebServer proxyServer = new MockWebServer();
        proxyServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String requestLine = request.getRequestLine();
                if (requestLine.contains("/mySegments")) {
                    mySegmentsHit.set(true);
                    return new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (requestLine.contains("splitChanges")) {
                    splitChangesHit.set(true);
                    return new MockResponse().setResponseCode(200).setBody(mSplitChanges);
                } else if (requestLine.contains("auth")) {
                    authLatch.countDown();
                    return new MockResponse().setResponseCode(200).setBody("{}");
                } else if (requestLine.contains("Impressions")) {
                    eventsLatch.countDown();
                    return new MockResponse().setResponseCode(200).setBody("{}");
                } else {
                    return new MockResponse().setResponseCode(200).setBody("{}");
                }
            }
        });
        proxyServer.start();

        MockWebServer baseServer = new MockWebServer();
        baseServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String requestLine = request.getRequestLine();
                return new MockResponse().setResponseCode(200);
            }
        });
        baseServer.start();

        String url = baseServer.url("").toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url)
                .eventsEndpoint(url)
                .sseAuthServiceEndpoint(url)
                .streamingServiceEndpoint(url)
                .build();

        // init db
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        SplitRoomDatabase splitRoomDatabase = DatabaseHelper.getTestDatabase(context);
        splitRoomDatabase.clearAllTables();

        // init factory
        Key key = IntegrationHelper.dummyUserKey();
        mSplitFactory = IntegrationHelper.buildFactory(IntegrationHelper.dummyApiKey(),
                key,
                new SplitClientConfig.Builder()
                        .serviceEndpoints(endpoints)
                        .impressionsMode(ImpressionsMode.DEBUG)
                        .impressionsQueueSize(1)
                        .impressionsChunkSize(1)
                        .impressionsPerPush(1)
                        .impressionsRefreshRate(1)
                        .proxyHost(proxyServer.url("").toString())
                        .logLevel(SplitLogLevel.VERBOSE)
                        .build(),
                context, null, splitRoomDatabase);

        // init client
        SplitClient client = mSplitFactory.client(key);
        CountDownLatch readyLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask readyTask = new TestingHelper.TestEventTask(readyLatch);
        client.on(SplitEvent.SDK_READY, readyTask);
        boolean await = readyLatch.await(10, TimeUnit.SECONDS);
        if (!await) {
            Assert.fail("SDK_READY event not received");
        }

        client.getTreatment("FACUNDO_TEST");
        client.getTreatment("FACUNDO_TEST");
        client.getTreatment("FACUNDO_TEST");
        Thread.sleep(500);
        client.flush();

        boolean awaitAuth = authLatch.await(10, TimeUnit.SECONDS);
        boolean awaitEvents = eventsLatch.await(10, TimeUnit.SECONDS);
        // assert
        assertTrue(await);
        assertTrue(mySegmentsHit.get());
        assertTrue(splitChangesHit.get());
        assertTrue(awaitAuth);
        assertTrue(awaitEvents);

        // teardown
        proxyServer.shutdown();
        baseServer.shutdown();
    }

    @Test
    public void authenticatorIsUsed() throws IOException, InterruptedException {

        AtomicBoolean mySegmentsHit = new AtomicBoolean(false);
        AtomicBoolean splitChangesHit = new AtomicBoolean(false);
        CountDownLatch authLatch = new CountDownLatch(1);
        CountDownLatch eventsLatch = new CountDownLatch(1);
        CountDownLatch proxyAuthorizationHeaderLatch = new CountDownLatch(1);

        // init server
        MockWebServer proxyServer = new MockWebServer();
        proxyServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String requestLine = request.getRequestLine();
                if (request.getHeader("Proxy-Authorization") != null) {
                    proxyAuthorizationHeaderLatch.countDown();
                }
                if (requestLine.contains("/mySegments")) {
                    mySegmentsHit.set(true);
                    return new MockResponse().setResponseCode((request.getHeader("Proxy-Authorization") != null) ? 200 : 407).setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (requestLine.contains("splitChanges")) {
                    splitChangesHit.set(true);
                    return new MockResponse().setResponseCode((request.getHeader("Proxy-Authorization") != null) ? 200 : 407).setBody(mSplitChanges);
                } else if (requestLine.contains("auth")) {
                    authLatch.countDown();
                    return new MockResponse().setResponseCode((request.getHeader("Proxy-Authorization") != null) ? 200 : 407).setBody(IntegrationHelper.streamingDisabledToken());
                } else if (requestLine.contains("Impressions")) {
                    eventsLatch.countDown();
                    return new MockResponse().setResponseCode((request.getHeader("Proxy-Authorization") != null) ? 200 : 407).setBody("{}");
                } else {
                    return new MockResponse().setResponseCode((request.getHeader("Proxy-Authorization") != null) ? 200 : 407).setBody("{}");
                }
            }
        });
        proxyServer.start();

        MockWebServer baseServer = new MockWebServer();
        baseServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String requestLine = request.getRequestLine();
                return new MockResponse().setResponseCode(200);
            }
        });
        baseServer.start();

        String url = baseServer.url("").toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url)
                .eventsEndpoint(url)
                .sseAuthServiceEndpoint(url)
                .streamingServiceEndpoint(url)
                .build();

        // init db
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        SplitRoomDatabase splitRoomDatabase = DatabaseHelper.getTestDatabase(context);
        splitRoomDatabase.clearAllTables();

        // init factory
        Key key = IntegrationHelper.dummyUserKey();
        mSplitFactory = IntegrationHelper.buildFactory(IntegrationHelper.dummyApiKey(),
                key,
                new SplitClientConfig.Builder()
                        .serviceEndpoints(endpoints)
                        .proxyAuthenticator(new SplitAuthenticator() {
                            @Override
                            public SplitAuthenticatedRequest authenticate(@NonNull SplitAuthenticatedRequest request) {
                                request.setHeader("Proxy-Authorization", "Bearer 1234567890");
                                return request;
                            }
                        })
                        .impressionsMode(ImpressionsMode.DEBUG)
                        .impressionsQueueSize(1)
                        .impressionsChunkSize(1)
                        .impressionsPerPush(1)
                        .impressionsRefreshRate(1)
                        .proxyHost(proxyServer.url("").toString())
                        .logLevel(SplitLogLevel.VERBOSE)
                        .build(),
                context, null, splitRoomDatabase);

        // init client
        SplitClient client = mSplitFactory.client(key);
        CountDownLatch readyLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask readyTask = new TestingHelper.TestEventTask(readyLatch);
        client.on(SplitEvent.SDK_READY, readyTask);
        boolean await = readyLatch.await(10, TimeUnit.SECONDS);
        if (!await) {
            Assert.fail("SDK_READY event not received");
        }

        client.getTreatment("FACUNDO_TEST");
        client.getTreatment("FACUNDO_TEST");
        client.getTreatment("FACUNDO_TEST");
        Thread.sleep(500);
        client.flush();

        boolean awaitAuth = authLatch.await(5, TimeUnit.SECONDS);
        boolean awaitEvents = eventsLatch.await(5, TimeUnit.SECONDS);
        boolean awaitProxyAuthorizationHeader = proxyAuthorizationHeaderLatch.await(5, TimeUnit.SECONDS);
        // assert
        assertTrue(mySegmentsHit.get());
        assertTrue(splitChangesHit.get());
        assertTrue(awaitAuth);
        assertTrue(awaitEvents);
        assertTrue(awaitProxyAuthorizationHeader);

        // teardown
        proxyServer.shutdown();
        baseServer.shutdown();
    }
}
