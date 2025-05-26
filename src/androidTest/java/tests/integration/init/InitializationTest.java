package tests.integration.init;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static helper.IntegrationHelper.buildFactory;
import static helper.IntegrationHelper.emptyAllSegments;
import static helper.IntegrationHelper.getSinceFromUri;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import helper.DatabaseHelper;
import helper.IntegrationHelper;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import tests.integration.shared.TestingHelper;

public class InitializationTest {

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private CountDownLatch mRequestCountdownLatch;
    private MockWebServer mWebServer;

    private AtomicBoolean mEventSent;
    private CountDownLatch mEventLatch;

    @Before
    public void setUp() {
        setupServer();
        mRequestCountdownLatch = new CountDownLatch(1);
        mEventSent = new AtomicBoolean(false);
        mEventLatch = new CountDownLatch(1);
    }

    @Test
    public void immediateClientRecreation() throws InterruptedException {
        SplitFactory factory = getFactory(false);
        SplitClient client = factory.client();
        client.track("some_event");

        CountDownLatch latch = new CountDownLatch(1);
        client.on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(latch));

        // Wait for the client to be ready
        boolean readyAwait = latch.await(5, TimeUnit.SECONDS);

        // Destroy it
        client.destroy();

        // Create a new client; it should be ready since it was created immediately
        CountDownLatch secondReadyLatch = new CountDownLatch(1);
        factory.client(new Key("new_key")).on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(secondReadyLatch));
        boolean awaitReady2 = secondReadyLatch.await(5, TimeUnit.SECONDS);

        mEventLatch.await(5, TimeUnit.SECONDS);

        assertTrue(readyAwait);
        assertTrue(awaitReady2);
        assertFalse(mEventSent.get());
    }

    @Test
    public void destroyOnFactoryCallsDestroyWithActiveClients() throws InterruptedException {
        SplitFactory factory = getFactory(false);
        SplitClient client = factory.client();
        client.track("some_event");

        CountDownLatch latch = new CountDownLatch(1);
        client.on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(latch));

        // Wait for the client to be ready
        boolean readyAwait = latch.await(5, TimeUnit.SECONDS);

        CountDownLatch secondReadyLatch = new CountDownLatch(1);
        factory.client(new Key("new_key")).on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(secondReadyLatch));
        // Wait for second client to be ready
        boolean awaitReady2 = secondReadyLatch.await(5, TimeUnit.SECONDS);

        // Destroy the factory
        factory.destroy();
        // Wait for events to be posted
        Thread.sleep(500);

        // Verify event was posted to indirectly verify that the factory was destroyed
        boolean factoryWasDestroyed = mEventSent.get();

        assertTrue(readyAwait);
        assertTrue(awaitReady2);
        assertTrue(factoryWasDestroyed);
    }

    private SplitFactory getFactory(boolean ready) throws InterruptedException {
        SplitFactory splitFactory = getSplitFactory();
        CountDownLatch latch = new CountDownLatch(1);
        mRequestCountdownLatch.countDown();

        splitFactory.client().on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(latch));
        if (ready) {
            boolean await = latch.await(5, TimeUnit.SECONDS);

            if (!await) {
                fail("Client was not ready");
            }
        }

        return splitFactory;
    }

    private SplitFactory getSplitFactory() {
        final String url = mWebServer.url("/").url().toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url)
                .eventsEndpoint(url)
                .telemetryServiceEndpoint(url)
                .build();
        SplitClientConfig config = new SplitClientConfig.Builder()
                .trafficType("user")
                .serviceEndpoints(endpoints)
                .streamingEnabled(false)
                .featuresRefreshRate(9999)
                .segmentsRefreshRate(9999)
                .impressionsRefreshRate(9999)
                .logLevel(SplitLogLevel.VERBOSE)
                .streamingEnabled(false)
                .build();

        return buildFactory(IntegrationHelper.dummyApiKey(), IntegrationHelper.dummyUserKey(), config,
                mContext, null, DatabaseHelper.getTestDatabase(mContext));
    }

    private void setupServer() {
        mWebServer = new MockWebServer();

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                mRequestCountdownLatch.await();
                Thread.sleep(200);
                Logger.e("Path is: " + request.getPath());
                if (request.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new MockResponse().setResponseCode(200).setBody(emptyAllSegments());
                } else if (request.getPath().contains("/" + IntegrationHelper.ServicePath.SPLIT_CHANGES)) {
                    String sinceFromUri = getSinceFromUri(request.getRequestUrl().uri());
                    if (sinceFromUri.equals("-1")) {
                        return new MockResponse().setResponseCode(200).setBody(loadSplitChanges());
                    } else {
                        return new MockResponse().setResponseCode(200)
                                .setBody(IntegrationHelper.emptySplitChanges(1506703262916L, 1506703262916L));
                    }
                } else if (request.getPath().contains("/" + IntegrationHelper.ServicePath.EVENTS)) {
                    mEventSent.set(true);
                    mEventLatch.countDown();
                    return new MockResponse().setResponseCode(200);
                } else if (request.getPath().contains("/" + IntegrationHelper.ServicePath.COUNT)) {
                    return new MockResponse().setResponseCode(200);
                } else if (request.getPath().contains("/" + IntegrationHelper.ServicePath.IMPRESSIONS)) {
                    return new MockResponse().setResponseCode(200);
                } else if (request.getPath().contains("/" + IntegrationHelper.ServicePath.UNIQUE_KEYS)) {
                    return new MockResponse().setResponseCode(200);
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }

    private String loadSplitChanges() {
        return IntegrationHelper.loadSplitChanges(mContext, "split_changes_1.json");
    }
}
