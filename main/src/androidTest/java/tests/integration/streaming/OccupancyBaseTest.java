package tests.integration.streaming;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;

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
import helper.TestingHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.utils.logger.Logger;
import fake.HttpStreamResponseMock;

public abstract class OccupancyBaseTest {

    private static final String PUBLISHERS_PLACEHOLDER = "$PUBLISHERS$";
    private static final String CHANNEL_PLACEHOLDER = "$CHANNEL$";
    private static final String TIMESTAMP_PLACEHOLDER = "$TIMESTAMP$";
    protected static final String PRIMARY_CHANNEL = "control_pri";
    protected static final String SECONDARY_CHANNEL = "control_sec";
    private static final String OCCUPANCY_MOCK_FILE = "push_msg-occupancy.txt";
    private Context mContext;
    private BlockingQueue<String> mStreamingData;
    private CountDownLatch mSseConnectLatch;
    private String mOccupancyMsgMock;
    protected int mMySegmentsHitCount;
    protected int mSplitsHitCount;
    protected TelemetryStorage mTelemetryStorage;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mMySegmentsHitCount = 0;
        mSplitsHitCount = 0;
        mSseConnectLatch = new CountDownLatch(1);
        loadOccupancyMsgMock();
    }

    private HttpResponseMockDispatcher createBasicResponseDispatcher() {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    Logger.i("** My segments hit");
                    mMySegmentsHitCount++;
                    return createResponse(200, IntegrationHelper.dummyAllSegments());

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

    protected void pushOccupancy(String channel, int publishers) {
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


    protected SplitFactory getSplitFactory() throws IOException, InterruptedException {
        return getSplitFactory(IntegrationHelper.lowRefreshRateConfig());
    }

    @NonNull
    protected SplitFactory getSplitFactory(SplitClientConfig config) throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());
        mTelemetryStorage = StorageFactory.getTelemetryStorage(true);
        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock, null, null,
                null, null, mTelemetryStorage);

        SplitClient client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);

        latch.await(40, TimeUnit.SECONDS);
        mSseConnectLatch.await(40, TimeUnit.SECONDS);
        TestingHelper.pushKeepAlive(mStreamingData);
        return splitFactory;
    }

    private HttpResponseMock createResponse(int status, String data) {
        return new HttpResponseMock(status, data);
    }

    private HttpStreamResponseMock createStreamResponse(int status, BlockingQueue<String> streamingResponseData) throws IOException {
        return new HttpStreamResponseMock(status, streamingResponseData);
    }
}
