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
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.utils.logger.Logger;

public class SyncGuardianIntegrationTest {

    private Context mContext;
    private BlockingQueue<String> mStreamingData;
    private CountDownLatch mMySegmentsHitsCountLatch;
    private CountDownLatch mSplitsHitsCountLatch;

    private boolean mIsStreamingAuth;
    private boolean mIsStreamingConnected;
    private int mySegmentsHitsCountHit;
    private int mSplitsHitsCountHit;
    private int mSseAuthHits;

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

        boolean readyAwait = latch.await(10, TimeUnit.SECONDS);
        if (!readyAwait) {
            Logger.e("SDK_READY event not received");
        }
        boolean mySegmentsAwait = mMySegmentsHitsCountLatch.await(10, TimeUnit.SECONDS);
        if (!mySegmentsAwait) {
            Logger.e("MySegments hits not received");
        }
        boolean splitsAwait = mSplitsHitsCountLatch.await(10, TimeUnit.SECONDS);
        if (!splitsAwait) {
            Logger.e("Splits hits not received");
        }

        Assert.assertTrue(client.isReady());
        Assert.assertTrue(readyTask.isOnPostExecutionCalled);

        Assert.assertEquals(1, mSseAuthHits);

        Assert.assertTrue(mIsStreamingConnected);


        splitFactory.destroy();
    }

    private HttpResponseMock createResponse(String data) {
        return new HttpResponseMock(200, data);
    }

    private HttpStreamResponseMock createStreamResponse(BlockingQueue<String> streamingResponseData) throws IOException {
        return new HttpStreamResponseMock(200, streamingResponseData);
    }

    private HttpResponseMockDispatcher createBasicResponseDispatcher() {
        return new HttpResponseMockDispatcher(){
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/mySegments")) {
                    Logger.i("** My segments hit");
                    mMySegmentsHitsCountLatch.countDown();
                    mySegmentsHitsCountHit++;
                    return createResponse(IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    Logger.i("** Split Changes hit");
                    mSplitsHitsCountLatch.countDown();
                    mSplitsHitsCountHit++;
                    String data = IntegrationHelper.emptySplitChanges(-1, 1000);
                    return createResponse(data);
                } else if (uri.getPath().contains("/auth")) {
                    Logger.i("** SSE Auth hit");
                    mIsStreamingAuth = true;
                    mSseAuthHits++;
                    return createResponse(IntegrationHelper.streamingEnabledToken());
                } else {
                    return new HttpResponseMock(200);
                }
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    Logger.i("** SSE Connect hit");
                    mIsStreamingConnected = true;
                    return createStreamResponse(mStreamingData);
                } catch (Exception e) {
                }
                return null;
            }
        };
    }
}
