package tests.integration.largesegments;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import helper.FileHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import tests.integration.shared.TestingHelper;

public class LargeSegmentTestHelper {

    protected final FileHelper mFileHelper = new FileHelper();
    protected final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();

    protected MockWebServer mWebServer;
    protected Map<String, AtomicInteger> mEndpointHits;
    protected Map<String, CountDownLatch> mLatches;
    protected final AtomicLong mMySegmentsDelay = new AtomicLong(0L);
    protected final AtomicLong mMyLargeSegmentsDelay = new AtomicLong(1000L);
    protected final AtomicInteger mMyLargeSegmentsStatusCode = new AtomicInteger(200);
    protected final AtomicBoolean mRandomizeMyLargeSegments = new AtomicBoolean(false);
    protected final AtomicBoolean mBrokenApi = new AtomicBoolean(false);

    @Before
    public void setUp() throws IOException {
        mEndpointHits = new ConcurrentHashMap<>();
        mMySegmentsDelay.set(0L);
        mMyLargeSegmentsDelay.set(1000L);
        mMyLargeSegmentsStatusCode.set(200);
        mRandomizeMyLargeSegments.set(false);
        mBrokenApi.set(false);
        initializeLatches();

        mWebServer = new MockWebServer();
        mWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                System.out.println("Receiving request to " + request.getRequestUrl().toString());

                if (mBrokenApi.get()) {
                    return new MockResponse().setResponseCode(500);
                }

                if (request.getRequestUrl().encodedPathSegments().contains("splitChanges")) {
                    updateEndpointHit("splitChanges");
                    return new MockResponse().setResponseCode(200).setBody(splitChangesLargeSegments(1602796638344L, 1602796638344L));
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
                        String body = IntegrationHelper.dummyMyLargeSegments();
                        if (mRandomizeMyLargeSegments.get()) {
                            body = IntegrationHelper.randomizedMyLargeSegments();
                        }

                        return new MockResponse().setResponseCode(200).setBody(body);
                    }

                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        });
        mWebServer.start();
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

    protected SplitFactory getFactory(boolean largeSegmentsEnabled, boolean waitForLargeSegments) {
        return getFactory(largeSegmentsEnabled, waitForLargeSegments, null, 2500, null);
    }

    protected SplitFactory getFactory(boolean largeSegmentsEnabled,
                                      boolean waitForLargeSegments,
                                      Integer largeSegmentsRefreshRate,
                                      Integer ready, SplitRoomDatabase database) {
        TestableSplitConfigBuilder configBuilder = new TestableSplitConfigBuilder()
                .largeSegmentsEnabled(largeSegmentsEnabled)
                .waitForLargeSegments(waitForLargeSegments)
                .enableDebug()
                .serviceEndpoints(ServiceEndpoints.builder()
                        .apiEndpoint("http://" + mWebServer.getHostName() + ":" + mWebServer.getPort())
                        .build());

        if (largeSegmentsRefreshRate != null) {
            configBuilder.streamingEnabled(false);
            configBuilder.largeSegmentsRefreshRate(largeSegmentsRefreshRate);
        }
        if (ready != null) {
            configBuilder.ready(ready);
        }
        return IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                configBuilder.build(),
                mContext,
                null, database, null, null, null);
    }

    protected SplitClient getReadyClient(String matchingKey, SplitFactory factory) {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        SplitClient client = factory.client(matchingKey);
        client.on(SplitEvent.SDK_READY, TestingHelper.testTask(countDownLatch));
        try {
            countDownLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return client;
    }

    private String splitChangesLargeSegments(long since, long till) {
        String change = mFileHelper.loadFileContent(mContext, "split_changes_large_segments-0.json");
        SplitChange parsedChange = Json.fromJson(change, SplitChange.class);
        parsedChange.since = since;
        parsedChange.till = till;

        return Json.toJson(parsedChange);
    }
}
