package tests.integration.streaming;

import static org.junit.Assert.assertTrue;

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
import helper.DatabaseHelper;
import fake.HttpStreamResponseMock;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.utils.logger.Logger;

import static java.lang.Thread.sleep;

public class SseAuthFail5xxTest {
    private Context mContext;
    private BlockingQueue<String> mStreamingData;
    private CountDownLatch mSseAuthLatch;
    private CountDownLatch mSseConnLatch;

    private boolean mIsStreamingAuth;
    private boolean mIsStreamingConnected;
    private int mMySegmentsHitsCountHit;
    private int mSplitsHitsCountHit;
    private int mAuthHits;
    private int mMySegmentsHitsCountHitAfterSseConn = 0;
    int mSplitsHitsCountHitAfterSseConn = 0;

    private static final int MAX_AUTH_RETRIES = 3;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mSseAuthLatch = new CountDownLatch(1);
        mSseConnLatch = new CountDownLatch(1);
        mIsStreamingAuth = false;
        mIsStreamingConnected = false;
        mMySegmentsHitsCountHit = 0;
        mSplitsHitsCountHit = 0;
        mMySegmentsHitsCountHitAfterSseConn = 0;
        mSplitsHitsCountHitAfterSseConn = 0;

        mAuthHits = 0;
    }

    @Test
    public void retryAuthOn5xx() throws IOException, InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.lowRefreshRateConfig();

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock, DatabaseHelper.getTestDatabase(mContext));

        SplitEventTask readyTask = new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                readyLatch.countDown();
            }
        };
        splitFactory.client().on(SplitEvent.SDK_READY, readyTask);

        boolean readyAwait = readyLatch.await(20, TimeUnit.SECONDS);
        if (!readyAwait) {
            Assert.fail("SDK not ready");
        }
        boolean sseAuthAwait = mSseAuthLatch.await(60, TimeUnit.SECONDS);
        if (!sseAuthAwait) {
            Assert.fail("Sse auth not ready");
        }
        boolean sseConnAwait = mSseConnLatch.await(20, TimeUnit.SECONDS);
        if (!sseConnAwait) {
            Assert.fail("Sse connection not ready");
        }

        sleep(2000);

        // More than 1 hits means polling enabled
        assertTrue(mMySegmentsHitsCountHit > 1);
        assertTrue(mSplitsHitsCountHit > 1);

        // Checking sse auth retries
        Assert.assertEquals(MAX_AUTH_RETRIES + 1, mAuthHits);
        // Checking streaming connection
        assertTrue(mIsStreamingAuth);
        assertTrue(mIsStreamingConnected);

        // More than 1 hit corresponding to full sync after streaming connection,
        // means polling still working after sse auth
        // Following lines are commented until a safe way to check this is found
//        Assert.assertEquals(1, mMySegmentsHitsCountHitAfterSseConn);
//        Assert.assertEquals(1, mSplitsHitsCountHitAfterSseConn);

        splitFactory.destroy();
    }

    private HttpResponseMock createResponse(int status, String data) {
        return new HttpResponseMock(status, data);
    }

    private HttpStreamResponseMock createStreamResponse(int status, BlockingQueue<String> streamingResponseData) throws IOException {
        return new HttpStreamResponseMock(status, streamingResponseData);
    }

    private HttpResponseMockDispatcher createBasicResponseDispatcher() {
        return new HttpResponseMockDispatcher(){
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/mySegments")) {
                    Logger.i("** My segments hit: " + mMySegmentsHitsCountHit);
                    if(!mIsStreamingConnected) {
                        mMySegmentsHitsCountHit++;
                    } else {
                        mMySegmentsHitsCountHitAfterSseConn++;
                    }
                    return createResponse(200, IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    Logger.i("** Split Changes hit: " + mSplitsHitsCountHit);
                    if(!mIsStreamingConnected) {
                        mSplitsHitsCountHit++;
                    } else {
                        mSplitsHitsCountHitAfterSseConn++;
                    }
                    String data = IntegrationHelper.emptySplitChanges(-1, 1000);
                    return createResponse(200, data);
                } else if (uri.getPath().contains("/auth")) {
                    Logger.i("** SSE Auth hit - Attempt: " + mAuthHits);
                    try {
                        sleep(20);
                        if(mAuthHits <= MAX_AUTH_RETRIES) {
                            mAuthHits++;
                            return createResponse(500, null);
                        } else {
                            mIsStreamingAuth = true;
                            mSseAuthLatch.countDown();
                            return createResponse(200, IntegrationHelper.streamingEnabledToken());
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return new HttpResponseMock(200);
                }
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    Logger.i("** SSE Connect hit");
                    mIsStreamingConnected = true;
                    mSseConnLatch.countDown();
                    return createStreamResponse(200, mStreamingData);
                } catch (Exception e) {
                }
                return null;
            }
        };
    }
}
