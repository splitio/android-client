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
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.utils.Logger;
import io.split.sharedtest.fake.HttpStreamResponseMock;

import static java.lang.Thread.sleep;

public class SseConnectionExpiredTokenTest {
    Context mContext;
    BlockingQueue<String> mStreamingData;
    CountDownLatch mSseExpiredTokenMessage;
    CountDownLatch mSseConnLatchExpiredToken;
    CountDownLatch mSseConnLatch;
    int mSseConnAuthHitCount;
    int mSseConnHitCount;
    HttpStreamResponseMock mExpiredStreamResponse;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        resetStreamingData();
        mSseExpiredTokenMessage = new CountDownLatch(1);
        mSseConnLatchExpiredToken = new CountDownLatch(1);
        mSseConnLatch = new CountDownLatch(1);
        mSseConnAuthHitCount = 0;
        mSseConnHitCount = 0;
    }

    private void resetStreamingData() {
        mStreamingData = new LinkedBlockingDeque<>();
    }

    @Test
    public void onSseTokenExpired() throws IOException, InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.lowRefreshRateConfig();

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock);

        SplitClient client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);

        client.on(SplitEvent.SDK_READY, readyTask);

        readyLatch.await(5, TimeUnit.SECONDS);

        // Wait for the first connection when expired token message is sent
        mSseConnLatchExpiredToken.await(5, TimeUnit.SECONDS);

        // Push token expired message and clouse connection
        pushTokenExpiredMessage();
        mSseExpiredTokenMessage.await(5, TimeUnit.SECONDS);
        mExpiredStreamResponse.close();


        // Wait to sdk to react to stream closed
        sleep(6000);

        // Wait for second connection to check full sse auth / sse connection cycle
        mSseConnLatch.await(5, TimeUnit.SECONDS);

        // Not considering hits when sending token expired
        // should be 1 hit for endpoint to count a full cycle
        Assert.assertEquals(2, mSseConnAuthHitCount);
        Assert.assertEquals(2, mSseConnHitCount);



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
                    return createResponse(200, IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    String data = IntegrationHelper.emptySplitChanges(-1, 1000);
                    return createResponse(200, data);
                } else if (uri.getPath().contains("/auth")) {
                    Logger.i("** SSE Auth hit: " + mSseConnAuthHitCount);
                    mSseConnAuthHitCount++;
                    return createResponse(200, IntegrationHelper.streamingEnabledToken());

                } else {
                    return new HttpResponseMock(200);
                }
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    Logger.i("** SSE Connect hit: " + mSseConnHitCount);
                    mSseConnHitCount++;
                    if (mSseConnHitCount == 1) {
                        mExpiredStreamResponse = createStreamResponse(200, mStreamingData);
                        mSseConnLatchExpiredToken.countDown();
                        return mExpiredStreamResponse;
                    } else {
                        mSseConnLatch.countDown();
                        return createStreamResponse(200, mStreamingData);
                    }
                } catch (Exception e) {
                }
                return null;
            }
        };
    }

    private void pushTokenExpiredMessage() {
        String message = loadMockedData("push_token-expired.txt");
        try {
            mStreamingData.put(message + "" + "\n");
            mSseExpiredTokenMessage.countDown();
            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }

    private String loadMockedData(String fileName) {
        FileHelper fileHelper = new FileHelper();
        return fileHelper.loadFileContent(mContext, fileName);
    }
}
