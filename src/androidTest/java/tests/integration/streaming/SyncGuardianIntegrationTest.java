package tests.integration.streaming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import io.split.android.client.utils.logger.Logger;

public class SyncGuardianIntegrationTest {

    private Context mContext;
    private BlockingQueue<String> mStreamingData;
    private CountDownLatch mMySegmentsHitsCountLatch;
    private CountDownLatch mSplitsHitsCountLatch;

    private CountDownLatch mIsStreamingConnected;
    private AtomicInteger mSplitsHitsCountHit;
    private AtomicInteger mSseAuthHits;
    private final LifecycleManagerStub mLifecycleManager = new LifecycleManagerStub();

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mSplitsHitsCountLatch = new CountDownLatch(1);
        mMySegmentsHitsCountLatch = new CountDownLatch(1);
        mIsStreamingConnected = new CountDownLatch(1);
        mSplitsHitsCountHit = new AtomicInteger(0);
        mSseAuthHits = new AtomicInteger(0);
    }

    @Test
    public void splitsAreNotFetchedWhenSSEConnectionIsActive() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Pair<SplitClient, SplitEventTaskHelper> pair = getClient(latch, true, false);
        SplitClient client = pair.first;
        SplitEventTaskHelper readyTask = pair.second;

        boolean mySegmentsAwait = mMySegmentsHitsCountLatch.await(10, TimeUnit.SECONDS);
        if (!mySegmentsAwait) {
            Logger.e("MySegments hits not received");
            fail();
        }

        boolean splitsAwait = mSplitsHitsCountLatch.await(10, TimeUnit.SECONDS);
        if (!splitsAwait) {
            Logger.e("Splits hits not received");
            fail();
        }

        boolean readyAwait = latch.await(10, TimeUnit.SECONDS);
        if (!readyAwait) {
            Logger.e("SDK_READY event not received");
        }

        boolean sseConnectionAwait = mIsStreamingConnected.await(10, TimeUnit.SECONDS);
        int initialSplitsHit = mSplitsHitsCountHit.get();

        mLifecycleManager.simulateOnPause();
        mLifecycleManager.simulateOnResume();

        int finalSplitsHit = mSplitsHitsCountHit.get();
        assertTrue(readyTask.isOnPostExecutionCalled);
        assertTrue(sseConnectionAwait);
        assertEquals(initialSplitsHit, finalSplitsHit);

        client.destroy();
    }

    @Test
    public void splitsAreNotFetchedWhenSSEConnectionIsInactiveAndTimeHasNotElapsed() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Pair<SplitClient, SplitEventTaskHelper> pair = getClient(latch, true, false);
        SplitClient client = pair.first;
        SplitEventTaskHelper readyTask = pair.second;

        boolean mySegmentsAwait = mMySegmentsHitsCountLatch.await(10, TimeUnit.SECONDS);
        if (!mySegmentsAwait) {
            Logger.e("MySegments hits not received");
            fail();
        }

        boolean splitsAwait = mSplitsHitsCountLatch.await(10, TimeUnit.SECONDS);
        if (!splitsAwait) {
            Logger.e("Splits hits not received");
            fail();
        }

        boolean readyAwait = latch.await(10, TimeUnit.SECONDS);
        if (!readyAwait) {
            Logger.e("SDK_READY event not received");
        }

        boolean sseConnectionAwait = mIsStreamingConnected.await(10, TimeUnit.SECONDS);
        int initialSplitsHit = mSplitsHitsCountHit.get();

        mLifecycleManager.simulateOnPause();
        mLifecycleManager.simulateOnResume();

        int finalSplitsHit = mSplitsHitsCountHit.get();
        assertTrue(readyTask.isOnPostExecutionCalled);
        assertTrue(sseConnectionAwait);
        assertEquals(initialSplitsHit, finalSplitsHit);

        client.destroy();
    }

    @Test
    public void splitsAreNotFetchedOnResumeWhenStreamingIsDisabled() throws InterruptedException, IOException {
        CountDownLatch latch = new CountDownLatch(1);

        Pair<SplitClient, SplitEventTaskHelper> pair = getClient(latch, false, false);
        SplitClient client = pair.first;
        SplitEventTaskHelper readyTask = pair.second;

        boolean mySegmentsAwait = mMySegmentsHitsCountLatch.await(10, TimeUnit.SECONDS);
        if (!mySegmentsAwait) {
            Logger.e("MySegments hits not received");
            fail();
        }

        boolean splitsAwait = mSplitsHitsCountLatch.await(10, TimeUnit.SECONDS);
        if (!splitsAwait) {
            Logger.e("Splits hits not received");
            fail();
        }

        boolean readyAwait = latch.await(10, TimeUnit.SECONDS);
        if (!readyAwait) {
            Logger.e("SDK_READY event not received");
        }

        int initialSplitsHit = mSplitsHitsCountHit.get();

        mLifecycleManager.simulateOnPause();
        mLifecycleManager.simulateOnResume();

        int finalSplitsHit = mSplitsHitsCountHit.get();
        assertTrue(readyTask.isOnPostExecutionCalled);
        assertEquals(0, mSseAuthHits.get());
        assertEquals(initialSplitsHit, finalSplitsHit);

        client.destroy();
    }

    @Test
    public void splitsAreNotFetchedOnResumeWhenSingleSyncIsEnabled() throws InterruptedException, IOException {
        CountDownLatch latch = new CountDownLatch(1);

        Pair<SplitClient, SplitEventTaskHelper> pair = getClient(latch, false, true);
        SplitClient client = pair.first;
        SplitEventTaskHelper readyTask = pair.second;

        boolean mySegmentsAwait = mMySegmentsHitsCountLatch.await(10, TimeUnit.SECONDS);
        if (!mySegmentsAwait) {
            Logger.e("MySegments hits not received");
            fail();
        }

        boolean splitsAwait = mSplitsHitsCountLatch.await(10, TimeUnit.SECONDS);
        if (!splitsAwait) {
            Logger.e("Splits hits not received");
            fail();
        }

        boolean readyAwait = latch.await(10, TimeUnit.SECONDS);
        if (!readyAwait) {
            Logger.e("SDK_READY event not received");
        }

        int initialSplitsHit = mSplitsHitsCountHit.get();

        mLifecycleManager.simulateOnPause();
        mLifecycleManager.simulateOnResume();

        int finalSplitsHit = mSplitsHitsCountHit.get();
        assertTrue(readyTask.isOnPostExecutionCalled);
        assertEquals(0, mSseAuthHits.get());
        assertEquals(initialSplitsHit, finalSplitsHit);

        client.destroy();
    }

    private Pair<SplitClient, SplitEventTaskHelper> getClient(CountDownLatch latch, boolean streamingEnabled, boolean singleSync) throws IOException {
        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = (singleSync) ? IntegrationHelper.syncDisabledConfig() : IntegrationHelper.lowRefreshRateConfig(streamingEnabled);

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                config,
                mContext,
                httpClientMock,
                null,
                null,
                null,
                mLifecycleManager);

        SplitClient client = splitFactory.client();
        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        client.on(SplitEvent.SDK_READY, readyTask);

        return new Pair<>(client, readyTask);
    }

    private HttpResponseMock createResponse(String data) {
        return new HttpResponseMock(200, data);
    }

    private HttpStreamResponseMock createStreamResponse(BlockingQueue<String> streamingResponseData) throws IOException {
        return new HttpStreamResponseMock(200, streamingResponseData);
    }

    private HttpResponseMockDispatcher createBasicResponseDispatcher() {
        return createStreamingResponseDispatcher(IntegrationHelper.streamingEnabledToken());
    }

    private HttpResponseMockDispatcher createStreamingResponseDispatcher(final String sseResponse) {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("mySegments")) {
                    Logger.i("** My segments hit");
                    mMySegmentsHitsCountLatch.countDown();
                    return createResponse(IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    Logger.i("** Split Changes hit");
                    mSplitsHitsCountLatch.countDown();
                    mSplitsHitsCountHit.incrementAndGet();
                    String data = IntegrationHelper.emptySplitChanges(-1, 1000);
                    return createResponse(data);
                } else if (uri.getPath().contains("/auth")) {
                    Logger.i("** SSE Auth hit");
                    mSseAuthHits.incrementAndGet();
                    return createResponse(sseResponse);
                } else {
                    return new HttpResponseMock(200);
                }
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    Logger.i("** SSE Connect hit");
                    mIsStreamingConnected.countDown();
                    return createStreamResponse(mStreamingData);
                } catch (Exception e) {
                }
                return null;
            }
        };
    }
}
