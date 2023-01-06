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
import io.split.sharedtest.fake.HttpStreamResponseMock;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.utils.logger.Logger;

public class StreamingDisabledInConfigTest {
    Context mContext;
    BlockingQueue<String> mStreamingData;
    CountDownLatch mMySegmentsHitsCountLatch;
    CountDownLatch mSplitsHitsCountLatch;

    boolean mIsStreamingAuth;
    boolean mIsStreamingConnected;
    int mySegmentsHitsCountHit;
    int mSplitsHitsCountHit;
    int mSseAuthHits;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mSplitsHitsCountLatch = new CountDownLatch(3);
        mMySegmentsHitsCountLatch = new CountDownLatch(3);
        mIsStreamingAuth = false;
        mIsStreamingConnected = false;
        mySegmentsHitsCountHit = 0;
        mSplitsHitsCountHit = 0;
        mSseAuthHits = 0;
    }

    @Test
    public void streamingDisabled() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.lowRefreshRateConfig(false);

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock);

        SplitClient client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);

        latch.await(40, TimeUnit.SECONDS);
        mMySegmentsHitsCountLatch.await(40, TimeUnit.SECONDS);
        mSplitsHitsCountLatch.await(40, TimeUnit.SECONDS);


        Assert.assertTrue(client.isReady());
        Assert.assertTrue(readyTask.isOnPostExecutionCalled);

        Assert.assertEquals(0, mSseAuthHits);

        // No streaming auth is made
        Assert.assertFalse(mIsStreamingAuth);

        // Checking no streaming connection
        Assert.assertFalse(mIsStreamingConnected);

        // More than 1 hits means polling enabled
        Assert.assertTrue( mySegmentsHitsCountHit > 1);
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
        return new HttpResponseMockDispatcher(){
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
                    long since = -1;
                    if (mSplitsHitsCountHit > 2) {
                        since = 1000;
                    } else if (mSplitsHitsCountHit > 1) {
                        since = 500;
                    }
                    String data = IntegrationHelper.emptySplitChanges(since, 1000);
                    return createResponse(200, data);
                } else if (uri.getPath().contains("/auth")) {
                    Logger.i("** SSE Auth hit");
                    mSseAuthHits++;
                    return createResponse(200, IntegrationHelper.streamingDisabledToken());
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
