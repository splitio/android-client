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
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.utils.Logger;

import static java.lang.Thread.sleep;

public class OccupancyTest {
    private static final String PUBLISHERS_PLACEHOLDER = "$PUBLISHERS$";
    private static final String CHANNEL_PLACEHOLDER = "$CHANNEL$";
    private static final String TIMESTAMP_PLACEHOLDER = "$TIMESTAMP$";
    private static final String PRIMARY_CHANNEL = "control_pri";
    private static final String SECONDARY_CHANNEL = "control_sec";
    private static final String OCCUPANCY_MOCK_FILE = "push_msg-occupancy.txt";
    private Context mContext;
    private BlockingQueue<String> mStreamingData;
    private CountDownLatch mSseConnectLatch;
    private String mOccupancyMsgMock;
    private int mMySegmentsHitCount;
    private int mSplitsHitCount;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mMySegmentsHitCount = 0;
        mSplitsHitCount = 0;
        mSseConnectLatch = new CountDownLatch(1);
        loadOccupancyMsgMock();
    }

    @Test
    public void occupancy() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.lowRefreshRateConfig();

        SplitFactory splitFactory = IntegrationHelper.buidFactory(
                IntegrationHelper.dummyApiKey(), IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock);

        SplitClient client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);

        latch.await(40, TimeUnit.SECONDS);
        mSseConnectLatch.await(40, TimeUnit.SECONDS);
        mMySegmentsHitCount = 0;
        mSplitsHitCount = 0;

        // Should disable streaming
        pushOccupancy(PRIMARY_CHANNEL, 0);
        sleep(4000);
        int mySegmentsHitCountAfterDisable = mMySegmentsHitCount;
        int splitsHitCountAfterDisable = mSplitsHitCount;

        pushOccupancy(SECONDARY_CHANNEL, 1);
        sleep(1000);
        mMySegmentsHitCount = 0;
        mSplitsHitCount = 0;
        sleep(4000);
        int mySegmentsHitCountAfterSecEnable = mMySegmentsHitCount;
        int splitsHitCountSecEnable = mSplitsHitCount;

        pushOccupancy(SECONDARY_CHANNEL, 0);
        sleep(300);
        mMySegmentsHitCount = 0;
        mSplitsHitCount = 0;
        sleep(4000);
        int mySegmentsHitCountAfterSecDisable = mMySegmentsHitCount;
        int splitsHitCountSecDisable = mSplitsHitCount;

        // Should enable streaming
        pushOccupancy(PRIMARY_CHANNEL, 1);
        sleep(1000);
        mMySegmentsHitCount = 0;
        mSplitsHitCount = 0;
        sleep(4000);
        int mySegmentsHitCountAfterEnable = mMySegmentsHitCount;
        int splitsHitCountAfterEnable = mSplitsHitCount;

        // Hits > 0 means polling enabled
        Assert.assertTrue(mySegmentsHitCountAfterDisable > 0);
        Assert.assertTrue(splitsHitCountAfterDisable > 0);

        // Hits == 2 means streaming enabled and sync all
        Assert.assertEquals(0,mySegmentsHitCountAfterSecEnable);
        Assert.assertEquals(0, splitsHitCountSecEnable);

        // Hits > 0 means secondary channel message ignored because pollling wasn't disabled
        Assert.assertTrue(mySegmentsHitCountAfterSecDisable > 0);
        Assert.assertTrue(splitsHitCountSecDisable > 0);

        // Hits == 0 means streaming enabled
        Assert.assertEquals(0, mySegmentsHitCountAfterEnable);
        Assert.assertEquals(0, splitsHitCountAfterEnable);

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
                    mMySegmentsHitCount++;
                    return createResponse(200, IntegrationHelper.dummyMySegments());

                } else if (uri.getPath().contains("/splitChanges")) {
                    Logger.i("** Split Changes hit");
                    mSplitsHitCount++;
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
                    mSseConnectLatch.countDown();
                    return createStreamResponse(200, mStreamingData);
                } catch (Exception e) {
                }
                return null;
            }
        };
    }

    private void loadOccupancyMsgMock() {
        FileHelper fileHelper = new FileHelper();
        mOccupancyMsgMock = fileHelper.loadFileContent(mContext, OCCUPANCY_MOCK_FILE);
    }

    private void pushOccupancy(String channel, int publishers) {
        String message = mOccupancyMsgMock
                .replace(CHANNEL_PLACEHOLDER, channel)
                .replace(TIMESTAMP_PLACEHOLDER, String.valueOf(System.currentTimeMillis()))
                .replace(PUBLISHERS_PLACEHOLDER, String.valueOf(publishers));
        try {
            mStreamingData.put(message + "" + "\n");

            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }
}
