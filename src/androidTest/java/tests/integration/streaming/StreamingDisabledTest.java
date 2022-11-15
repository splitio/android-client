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
import io.split.android.client.api.Key;
import io.split.sharedtest.fake.HttpStreamResponseMock;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.utils.logger.Logger;

public class StreamingDisabledTest {
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

        SplitClientConfig config = IntegrationHelper.lowRefreshRateConfig();

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
        Assert.assertTrue(splitFactory.isReady());
        Assert.assertTrue(readyTask.isOnPostExecutionCalled);

        // No streaming auth is made
        Assert.assertEquals(1, mSseAuthHits);

        // Checking no streaming connection
        Assert.assertFalse(mIsStreamingConnected);

        // More than 1 hits means polling enabled
        Thread.sleep(500);
        Assert.assertEquals(3,  mySegmentsHitsCountHit);
        Assert.assertEquals(3, mSplitsHitsCountHit);

        splitFactory.destroy();
    }

    @Test
    public void disabledStreamingWithMultipleClients() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        mMySegmentsHitsCountLatch = new CountDownLatch(6);
        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.lowRefreshRateConfig();

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock);

        SplitClient client = splitFactory.client();
        SplitClient client2 = splitFactory.client(new Key("key2"));

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        SplitEventTaskHelper readyTask2 = new SplitEventTaskHelper(latch2);

        client.on(SplitEvent.SDK_READY, readyTask);
        client2.on(SplitEvent.SDK_READY, readyTask2);

        latch.await(40, TimeUnit.SECONDS);
        mMySegmentsHitsCountLatch.await(40, TimeUnit.SECONDS);
        mSplitsHitsCountLatch.await(40, TimeUnit.SECONDS);

        Assert.assertTrue(client.isReady());
        Assert.assertTrue(client2.isReady());
        Assert.assertTrue(readyTask.isOnPostExecutionCalled);
        Assert.assertTrue(readyTask2.isOnPostExecutionCalled);

        // No streaming auth is made
        Assert.assertEquals(1, mSseAuthHits);

        // Checking no streaming connection
        Assert.assertFalse(mIsStreamingConnected);

        // More than 1 hits means polling enabled
        Assert.assertEquals(6,  mySegmentsHitsCountHit);
        Assert.assertEquals(3, mSplitsHitsCountHit);

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
                    String data = IntegrationHelper.emptySplitChanges(-1, 1000);
                    return createResponse(200, data);
                } else if (uri.getPath().contains("/auth")) {
                    Logger.i("** SSE Auth hit");
                    mIsStreamingAuth = true;
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
