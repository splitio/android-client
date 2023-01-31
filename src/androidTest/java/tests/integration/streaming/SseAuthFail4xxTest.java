package tests.integration.streaming;

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
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.utils.logger.Logger;

public class SseAuthFail4xxTest {
    Context mContext;
    BlockingQueue<String> mStreamingData;
    CountDownLatch mSseAuthLatch;
    CountDownLatch mMySegmentsHitsCountLatch;
    CountDownLatch mSplitsHitsCountLatch;

    boolean mIsStreamingAuth;
    boolean mIsStreamingConnected;
    int mySegmentsHitsCountHit;
    int mSplitsHitsCountHit;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mSseAuthLatch = new CountDownLatch(1);
        mSplitsHitsCountLatch = new CountDownLatch(3);
        mMySegmentsHitsCountLatch = new CountDownLatch(3);
        mIsStreamingAuth = false;
        mIsStreamingConnected = false;
        mySegmentsHitsCountHit = 0;
        mSplitsHitsCountHit = 0;
    }

    @Test
    public void disableWhen4xx() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.lowRefreshRateConfig();

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock);

        SplitClient client = splitFactory.client();

        SplitEventTask readyTask = new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                latch.countDown();
            }
        };

        client.on(SplitEvent.SDK_READY, readyTask);

        boolean await = latch.await(40, TimeUnit.SECONDS);
        mSseAuthLatch.await(40, TimeUnit.SECONDS);
        mMySegmentsHitsCountLatch.await(40, TimeUnit.SECONDS);
        mSplitsHitsCountLatch.await(40, TimeUnit.SECONDS);

        Assert.assertTrue(await);
        Assert.assertTrue(mIsStreamingAuth);
        Assert.assertTrue(client.isReady());

        // Checking no streaming connection
        Assert.assertFalse(mIsStreamingConnected);

        // More than 1 hits means polling enabled
        Assert.assertTrue(mySegmentsHitsCountHit > 1);
        Assert.assertTrue(mSplitsHitsCountHit > 1);

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
                    Logger.i("** My segments hit");
                    mMySegmentsHitsCountLatch.countDown();
                    mySegmentsHitsCountHit++;
                    return createResponse(200, IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    Logger.i("** Split Changes hit");
                    mSplitsHitsCountLatch.countDown();
                    mSplitsHitsCountHit++;
                    String data = IntegrationHelper.emptySplitChanges(-1, 1000);
                    return createResponse(200, data);
                } else if (uri.getPath().contains("/auth")) {
                    Logger.i("** SSE Auth hit");
                    mIsStreamingAuth = true;
                    mSseAuthLatch.countDown();
                    return createResponse(401, null);
                } else {
                    return new HttpResponseMock(200);
                }
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    Logger.i("** SSE Connect hit");
                    mIsStreamingConnected = true;
                    return createStreamResponse(200, mStreamingData);
                } catch (Exception e) {
                }
                return null;
            }
        };
    }
}
