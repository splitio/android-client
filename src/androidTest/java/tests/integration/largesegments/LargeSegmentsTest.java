package tests.integration.largesegments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class LargeSegmentsTest {

    private Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private MockWebServer mWebServer;
    private Map<String, AtomicInteger> mEndpointHits;
    private Map<String, CountDownLatch> mLatches;
    private AtomicLong mMySegmentsDelay = new AtomicLong(0L);
    private AtomicLong mMyLargeSegmentsDelay = new AtomicLong(1000L);
    private AtomicInteger mMyLargeSegmentsStatusCode = new AtomicInteger(200);

    @Before
    public void setUp() throws IOException {
        mEndpointHits = new ConcurrentHashMap<>();
        mMySegmentsDelay = new AtomicLong(0L);
        mMyLargeSegmentsDelay = new AtomicLong(1000L);
        mMyLargeSegmentsStatusCode = new AtomicInteger(200);
        initializeLatches();

        mWebServer = new MockWebServer();
        mWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                System.out.println("Receiving request to " + request.getRequestUrl().toString());

                if (request.getRequestUrl().encodedPathSegments().contains("splitChanges")) {
                    updateEndpointHit("splitChanges");
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.emptySplitChanges(1602796638344L, 1602796638344L));
                } else if (request.getRequestUrl().encodedPathSegments().contains("mySegments")) {
                    Thread.sleep(mMySegmentsDelay.get());
                    updateEndpointHit("mySegments");
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.dummyMySegments());
                } else if (request.getRequestUrl().encodedPathSegments().contains("myLargeSegments")) {
                    Thread.sleep(mMyLargeSegmentsDelay.get());
                    updateEndpointHit("myLargeSegments");
                    if (mMyLargeSegmentsStatusCode.get() != 200) {
                        return new MockResponse().setResponseCode(mMyLargeSegmentsStatusCode.get());
                    } else {
                        return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.dummyMyLargeSegments());
                    }
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        });
        mWebServer.start();
    }

    @Test
    public void sdkReadyIsEmittedAfterLargeSegmentsAreSyncedWhenWaitForLargeSegmentsIsTrue() {
        SplitFactory factory = getFactory(true, true, 0);

        getReadyClient(IntegrationHelper.dummyUserKey().matchingKey(), factory);

        factory.destroy();

        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
        assertEquals(0, mEndpointHits.get("myLargeSegments").get());
    }

    @Test
    public void sdkReadyTimeoutIsEmittedWhenWaitForLargeSegmentsIsTrueAndSyncFails() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicBoolean sdkReadyTimeout = new AtomicBoolean(false);
        mMyLargeSegmentsStatusCode.set(403);
        SplitFactory factory = getFactory(true, true, 0);

        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                countDownLatch.countDown();
            }
        });
        client.on(SplitEvent.SDK_READY_TIMED_OUT, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                sdkReadyTimeout.set(true);
                countDownLatch.countDown();
            }
        });
        boolean readyawait = countDownLatch.await(5, TimeUnit.SECONDS);
        mLatches.get("splitChanges").await(5, TimeUnit.SECONDS);
        mLatches.get("myLargeSegments").await(5, TimeUnit.SECONDS);
        mLatches.get("mySegments").await(5, TimeUnit.SECONDS);

        assertTrue(readyawait);
        assertTrue(sdkReadyTimeout.get());
        assertEquals(1, mEndpointHits.get("splitChanges").get());
        assertEquals(1, mEndpointHits.get("mySegments").get());
        assertEquals(1, mEndpointHits.get("myLargeSegments").get());

        factory.destroy();
    }

    @After
    public void tearDown() throws IOException {
        mWebServer.close();
    }

    private void initializeLatches() {
        mLatches = new ConcurrentHashMap<>();
        mLatches.put("splitChanges", new CountDownLatch(1));
        mLatches.put("mySegments", new CountDownLatch(1));
        mLatches.put("myLargeSegments", new CountDownLatch(1));
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

    private SplitFactory getFactory(boolean largeSegmentsEnabled, boolean waitForLargeSegments, int largeSegmentsRefreshRate) {
        return IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                new TestableSplitConfigBuilder()
                        .largeSegmentsEnabled(largeSegmentsEnabled)
                        .waitForLargeSegments(waitForLargeSegments)
                        .enableDebug()
                        .ready(2500)
                        .largeSegmentsRefreshRate(largeSegmentsRefreshRate)
                        .serviceEndpoints(ServiceEndpoints.builder()
                                .apiEndpoint("http://" + mWebServer.getHostName() + ":" + mWebServer.getPort())
                                .build())
                        .build(),
                mContext,
                null, null, null, null, null);
    }

    private SplitClient getReadyClient(String matchingKey, SplitFactory factory) {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        SplitClient client = factory.client(matchingKey);
        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return client;
    }
}
