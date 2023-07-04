package tests.integration.streaming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.core.util.Pair;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import fake.HttpStreamResponseMock;
import fake.LifecycleManagerStub;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.telemetry.model.UpdatesFromSSE;
import io.split.android.client.telemetry.storage.InMemoryTelemetryStorage;
import io.split.android.client.utils.logger.Logger;

public class SplitChangeNotificationIntegrationTest {
    private Context mContext;
    private BlockingQueue<String> mStreamingData;
    private CountDownLatch mMySegmentsHitsCountLatch;
    private CountDownLatch mSplitsHitsCountLatch;

    private CountDownLatch mIsStreamingConnected;
    private AtomicInteger mSplitsHitsCountHit;
    private AtomicInteger mSseAuthHits;
    private final LifecycleManagerStub mLifecycleManager = new LifecycleManagerStub();
    private InMemoryTelemetryStorage mTelemetryStorage;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mSplitsHitsCountLatch = new CountDownLatch(1);
        mMySegmentsHitsCountLatch = new CountDownLatch(1);
        mIsStreamingConnected = new CountDownLatch(1);
        mSplitsHitsCountHit = new AtomicInteger(0);
        mSseAuthHits = new AtomicInteger(0);
        mTelemetryStorage = new InMemoryTelemetryStorage();
    }

    @Test
    public void notificationWithCompressionType0IsCorrectlySaved() throws IOException, InterruptedException {
        testSplitNotification(IntegrationHelper.splitChangeV2CompressionType0());
    }

    @Test
    public void notificationWithCompressionType1IsCorrectlySaved() throws IOException, InterruptedException {
        testSplitNotification(IntegrationHelper.splitChangeV2CompressionType1());
    }

    @Test
    public void notificationWithCompressionType2IsCorrectlySaved() throws IOException, InterruptedException {
        testSplitNotification(IntegrationHelper.splitChangeV2CompressionType2());
    }

    @Test
    public void telemetryForSplitsIsRecorded() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);

        Pair<SplitClient, SplitEventTaskHelper> pair = getClient(latch, IntegrationHelper.streamingEnabledToken());
        SplitClient client = pair.first;
        SplitEventTaskHelper readyTask = pair.second;
        client.on(SplitEvent.SDK_UPDATE, new SplitEventTaskHelper(updateLatch));
        awaitInitialization(latch);

        // wait for SSE to connect
        boolean sseConnectionAwait = mIsStreamingConnected.await(10, TimeUnit.SECONDS);

        // get first treatment; feature flag is not present
        String firstTreatment = client.getTreatment("mauro_java");

        // simulate SSE notification
        pushMessage(IntegrationHelper.splitChangeV2CompressionType0());

        boolean updateLatchAwait = updateLatch.await(10, TimeUnit.SECONDS);

        String secondTreatment = client.getTreatment("mauro_java");

        assertTrue(readyTask.isOnPostExecutionCalled);
        assertTrue(updateLatchAwait);
        assertTrue(sseConnectionAwait);
        assertEquals("control", firstTreatment);
        assertEquals("off", secondTreatment);
        Thread.sleep(500);
        UpdatesFromSSE updatesFromSSE = mTelemetryStorage.popUpdatesFromSSE();
        assertEquals(1, updatesFromSSE.getSplits());
        assertEquals(0, updatesFromSSE.getMySegments());
        client.destroy();
    }

    private void testSplitNotification(String notificationMessage) throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);

        Pair<SplitClient, SplitEventTaskHelper> pair = getClient(latch, IntegrationHelper.streamingEnabledToken());
        SplitClient client = pair.first;
        SplitEventTaskHelper readyTask = pair.second;
        client.on(SplitEvent.SDK_UPDATE, new SplitEventTaskHelper(updateLatch));
        awaitInitialization(latch);

        // wait for SSE to connect
        boolean sseConnectionAwait = mIsStreamingConnected.await(10, TimeUnit.SECONDS);

        // get first treatment; feature flag is not present
        String firstTreatment = client.getTreatment("mauro_java");

        // simulate SSE notification
        pushMessage(notificationMessage);

        boolean updateLatchAwait = updateLatch.await(10, TimeUnit.SECONDS);

        String secondTreatment = client.getTreatment("mauro_java");

        assertTrue(readyTask.isOnPostExecutionCalled);
        assertTrue(updateLatchAwait);
        assertTrue(sseConnectionAwait);
        assertEquals("control", firstTreatment);
        assertEquals("off", secondTreatment);

        client.destroy();
    }

    private Pair<SplitClient, SplitEventTaskHelper> getClient(CountDownLatch latch, String sseResponse) throws IOException, InterruptedException {
        HttpClientMock httpClientMock = new HttpClientMock(createStreamingResponseDispatcher(sseResponse));

        SplitClientConfig config = IntegrationHelper.customSseConnectionDelayConfig(true, 0, 5L);

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                config,
                mContext,
                httpClientMock,
                null,
                null,
                null,
                mLifecycleManager,
                mTelemetryStorage);

        SplitClient client = splitFactory.client();
        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        client.on(SplitEvent.SDK_READY, readyTask);

        return new Pair<>(client, readyTask);
    }

    private void awaitInitialization(CountDownLatch latch) throws InterruptedException {
        if (!mMySegmentsHitsCountLatch.await(10, TimeUnit.SECONDS)) {
            Logger.e("MySegments hits not received");
            fail();
        }

        if (!mSplitsHitsCountLatch.await(10, TimeUnit.SECONDS)) {
            Logger.e("Splits hits not received");
            fail();
        }

        if (!latch.await(10, TimeUnit.SECONDS)) {
            Logger.e("SDK_READY event not received");
        }
    }

    private HttpResponseMock createResponse(String data) {
        return new HttpResponseMock(200, data);
    }

    private HttpStreamResponseMock createStreamResponse(BlockingQueue<String> streamingResponseData) throws IOException {
        return new HttpStreamResponseMock(200, streamingResponseData);
    }

    private HttpResponseMockDispatcher createStreamingResponseDispatcher(final String sseResponse) {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("mySegments")) {
                    mMySegmentsHitsCountLatch.countDown();
                    return createResponse(IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    mSplitsHitsCountLatch.countDown();
                    mSplitsHitsCountHit.incrementAndGet();
                    String data = IntegrationHelper.emptySplitChanges(-1, 1000);
                    return createResponse(data);
                } else if (uri.getPath().contains("/auth")) {
                    mSseAuthHits.incrementAndGet();
                    return createResponse(sseResponse);
                } else {
                    return new HttpResponseMock(200);
                }
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    mIsStreamingConnected.countDown();
                    return createStreamResponse(mStreamingData);
                } catch (Exception e) {
                }
                return null;
            }
        };
    }

    private void pushMessage(String message) {

        try {
            mStreamingData.put(message + "" + "\n");

            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }
}
