package tests.integration.toggle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static helper.IntegrationHelper.buildFactory;
import static helper.IntegrationHelper.emptyAllSegments;
import static helper.IntegrationHelper.getSinceFromUri;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitManager;
import io.split.android.client.api.SplitView;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import tests.integration.shared.TestingHelper;

public class ImpressionsToggleTest {

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private CountDownLatch mRequestCountdownLatch;
    private MockWebServer mWebServer;

    private final AtomicReference<String> mCountBody = new AtomicReference<>(null);
    private final AtomicReference<String> mImpressionsBody = new AtomicReference<>(null);
    private final AtomicReference<String> mUniqueKeysBody = new AtomicReference<>(null);

    private CountDownLatch mCountLatch;
    private CountDownLatch mImpressionsLatch;
    private CountDownLatch mUniqueKeysLatch;

    @Before
    public void setUp() {
        setupServer();
        mRequestCountdownLatch = new CountDownLatch(1);
        mCountBody.set(null);
        mImpressionsBody.set(null);
        mUniqueKeysBody.set(null);

        mCountLatch = new CountDownLatch(1);
        mImpressionsLatch = new CountDownLatch(1);
        mUniqueKeysLatch = new CountDownLatch(1);
    }

    @Test
    public void managerContainsProperty() throws InterruptedException {
        SplitFactory splitFactory = getReadyFactory(ImpressionsMode.OPTIMIZED);

        SplitManager manager = splitFactory.manager();
        List<SplitView> splits = manager.splits();

        assertTrue(manager.split("tracked").trackImpressions);
        assertFalse(manager.split("not_tracked").trackImpressions);
        assertEquals(2, splits.size());
    }

    @Test
    public void testNoneMode() throws InterruptedException {
        // 1. Initialize SDK in impressions NONE mode
        SplitFactory splitFactory = getReadyFactory(ImpressionsMode.NONE);

        // 2. Fetch splitChanges with both flags with trackImpressions true & false
        SplitClient client = splitFactory.client();
        String trackedTreatment = client.getTreatment("tracked");
        String notTrackedTreatment = client.getTreatment("not_tracked");
        Thread.sleep(200);

        // 3. Verify all counts & mtks are tracked
        client.flush();

        mUniqueKeysLatch.await(5, TimeUnit.SECONDS);
        mCountLatch.await(5, TimeUnit.SECONDS);

        assertNotNull(mCountBody.get());
        assertNotNull(mUniqueKeysBody.get());
        assertNull(mImpressionsBody.get());
    }

    @Test
    public void test2() {
        // 1. Initialize SDK in impressions DEBUG mode
        // 2. Fetch splitChanges with both flags with trackImpressions true & false
        // 3. Verify counts & MTKs are tracked only for trackImpressions false
        // 4. Verify impressions are tracked for trackImpressions true
    }

    @Test
    public void test3() {
        // 1. Initialize SDK in impressions OPTIMIZED mode
        // 2. Fetch splitChanges with both flags with trackImpressions true & false
        // 3. Verify counts & MTKs are tracked only for trackImpressions false
        // 4. Verify impressions are tracked for trackImpressions true
    }

    private SplitFactory getReadyFactory(ImpressionsMode impressionsMode) throws InterruptedException {
        SplitFactory splitFactory = getSplitFactory(impressionsMode);
        CountDownLatch latch = new CountDownLatch(1);
        splitFactory.client().on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(latch));
        mRequestCountdownLatch.countDown();

        boolean await = latch.await(5, TimeUnit.SECONDS);

        if (!await) {
            fail("Client was not ready");
        }

        return splitFactory;
    }

    private SplitFactory getSplitFactory(ImpressionsMode impressionsMode) {
        final String url = mWebServer.url("/").url().toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url)
                .eventsEndpoint(url)
                .telemetryServiceEndpoint(url)
                .build();
        SplitClientConfig config = new SplitClientConfig.Builder()
                .serviceEndpoints(endpoints)
                .impressionsMode(impressionsMode)
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
                } else if (request.getPath().contains("/" + IntegrationHelper.ServicePath.COUNT)) {
                    mCountLatch.countDown();
                    mCountBody.set(request.getBody().readUtf8());
                    return new MockResponse().setResponseCode(200);
                } else if (request.getPath().contains("/" + IntegrationHelper.ServicePath.IMPRESSIONS)) {
                    mImpressionsLatch.countDown();
                    mImpressionsBody.set(request.getBody().readUtf8());
                    return new MockResponse().setResponseCode(200);
                } else if (request.getPath().contains("/" + IntegrationHelper.ServicePath.UNIQUE_KEYS)) {
                    mUniqueKeysLatch.countDown();
                    mUniqueKeysBody.set(request.getBody().readUtf8());
                    return new MockResponse().setResponseCode(200);
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }

    private String loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        String change = fileHelper.loadFileContent(mContext, "split_changes_imp_toggle.json");
        SplitChange parsedChange = Json.fromJson(change, SplitChange.class);
        parsedChange.since = parsedChange.till;
        return Json.toJson(parsedChange);
    }
}
