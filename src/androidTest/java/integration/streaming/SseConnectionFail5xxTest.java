package integration.streaming;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import fake.HttpStreamResponseMock;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.utils.Logger;

import static java.lang.Thread.sleep;

public class SseConnectionFail5xxTest {
    Context mContext;
    BlockingQueue<String> mStreamingData;
    CountDownLatch mSseConnLatch;

    boolean mIsStreamingConnected;
    int mMySegmentsHitsCountHit;
    int mSplitsHitsCountHit;
    int mSseConnHits;
    int mMySegmentsHitsCountHitAfterSseConn = 0;
    int mSplitsHitsCountHitAfterSseConn = 0;

    private static final int MAX_SSE_CONN_RETRIES = 3;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mSseConnLatch = new CountDownLatch(1);
        mIsStreamingConnected = false;
        mMySegmentsHitsCountHit = 0;
        mSplitsHitsCountHit = 0;
        mMySegmentsHitsCountHitAfterSseConn = 0;
        mSplitsHitsCountHitAfterSseConn = 0;

        mSseConnHits = 0;
    }

    @Test
    public void retryOnSseConn5xx() throws IOException, InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.lowRefreshRateConfig();

        SplitFactory splitFactory = IntegrationHelper.buidFactory(
                IntegrationHelper.dummyApiKey(), IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock);

        SplitClient client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);

        client.on(SplitEvent.SDK_READY, readyTask);

        readyLatch.await(40, TimeUnit.SECONDS);

        mSseConnLatch.await(40, TimeUnit.SECONDS);

        sleep(5000);

        Assert.assertTrue(client.isReady());
        Assert.assertTrue(splitFactory.isReady());
        Assert.assertTrue(readyTask.isOnPostExecutionCalled);

        // More than 1 hits means polling enabled
        Assert.assertTrue(mMySegmentsHitsCountHit > 1);
        Assert.assertTrue(mSplitsHitsCountHit > 1);

        // Checking sse auth retries
        Assert.assertEquals(MAX_SSE_CONN_RETRIES + 1, mSseConnHits);
        // Checking streaming connection
        Assert.assertTrue(mIsStreamingConnected);

        // More than 1 hit corresponding to full sync after streaming connection,
        // means polling still working after sse auth
        Assert.assertEquals(1, mMySegmentsHitsCountHitAfterSseConn);
        Assert.assertEquals(1, mSplitsHitsCountHitAfterSseConn);

        splitFactory.destroy();
    }

    private HttpResponseMock createResponse(int status, String data) {
        return new HttpResponseMock(status, data);
    }

    private HttpStreamResponseMock createStreamResponse(int status, BlockingQueue<String> streamingResponseData) throws IOException {
        return new HttpStreamResponseMock(status, streamingResponseData);
    }

    private HttpResponseMockDispatcher createBasicResponseDispatcher() {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/mySegments")) {
                    Logger.i("** My segments hit: " + mMySegmentsHitsCountHit);
                    if (!mIsStreamingConnected) {
                        mMySegmentsHitsCountHit++;
                    } else {
                        mMySegmentsHitsCountHitAfterSseConn++;
                    }
                    return createResponse(200, IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    Logger.i("** Split Changes hit: " + mSplitsHitsCountHit);
                    if (!mIsStreamingConnected) {
                        mSplitsHitsCountHit++;
                    } else {
                        mSplitsHitsCountHitAfterSseConn++;
                    }
                    String data = IntegrationHelper.emptySplitChanges(-1, 1000);
                    return createResponse(200, data);
                } else if (uri.getPath().contains("/auth")) {
                    Logger.i("** SSE Auth hit");
                    return createResponse(200, IntegrationHelper.streamingEnabledToken());

                } else {
                    return new HttpResponseMock(200);
                }
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    Logger.i("** SSE Connect hit");
                    if (mSseConnHits <= MAX_SSE_CONN_RETRIES) {
                        mSseConnHits++;
                        return createStreamResponse(500, null);
                    } else {
                        mIsStreamingConnected = true;
                        mSseConnLatch.countDown();
                        return createStreamResponse(200, mStreamingData);
                    }
                } catch (Exception e) {
                }
                return null;
            }
        };
    }
}
