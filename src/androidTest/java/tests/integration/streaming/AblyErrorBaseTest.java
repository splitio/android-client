package tests.integration.streaming;

import android.content.Context;

import androidx.core.util.Pair;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
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
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestingHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.utils.logger.Logger;
import fake.HttpStreamResponseMock;

public abstract class AblyErrorBaseTest {
    private Context mContext;
    private BlockingQueue<String> mStreamingData;
    private HttpStreamResponseMock mStreamingResponse;
    private CountDownLatch mSplitsSyncLatch;
    private CountDownLatch mMySegmentsSyncLatch;
    private String mApiKey;

    protected CountDownLatch mSseLatch;

    protected int mSseHitCount = 0;
    protected TelemetryStorage mTelemetryStorage;

    SplitFactory mFactory;
    SplitClient mClient;

    SplitRoomDatabase mSplitRoomDatabase;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mSplitsSyncLatch = new CountDownLatch(2);
        mMySegmentsSyncLatch = new CountDownLatch(2);

        Pair<String, String> apiKeyAndDb = IntegrationHelper.dummyApiKeyAndDb();
        mApiKey = apiKeyAndDb.first;
        String dataFolderName = apiKeyAndDb.second;
        mSplitRoomDatabase = SplitRoomDatabase.getDatabase(mContext, dataFolderName);
        mSplitRoomDatabase.clearAllTables();
    }

    protected void initializeFactory() throws IOException, InterruptedException {
        initializeFactory(IntegrationHelper.basicConfig());
    }

    protected void initializeFactory(SplitClientConfig config) throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        mSseLatch = new CountDownLatch(1);

        SplitRoomDatabase db = DatabaseHelper.getTestDatabase(mContext);
        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());
        mTelemetryStorage = StorageFactory.getTelemetryStorage(true);
        mFactory = IntegrationHelper.buildFactory(
                mApiKey, IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock, db, null, null,
                null, mTelemetryStorage);

        mClient = mFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);

        mClient.on(SplitEvent.SDK_READY, readyTask);

        latch.await(5, TimeUnit.SECONDS);

        mSseLatch.await(5, TimeUnit.SECONDS);
    }

    protected void pushErrorMessage(int code) throws IOException, InterruptedException {
        mSseLatch = new CountDownLatch(1);
        pushMessage("push_msg-ably_error_" + code + ".txt");
        mStreamingData.put("\0");
        mSseLatch.await(5, TimeUnit.SECONDS);
        TestingHelper.delay(500);
//        mStreamingResponse.close();
    }

    @After
    public void tearDown() {
        mFactory.destroy();
    }

    private HttpResponseMock createResponse(int status, String data) {
        return new HttpResponseMock(status, data);
    }

    private HttpStreamResponseMock createStreamResponse(int status, BlockingQueue<String> streamingResponseData) throws IOException {
        mStreamingResponse = new HttpStreamResponseMock(status, streamingResponseData);
        return mStreamingResponse;
    }

    private HttpResponseMockDispatcher createBasicResponseDispatcher() {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/mySegments")) {
                    mMySegmentsSyncLatch.countDown();

                    return createResponse(200, IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    mSplitsSyncLatch.countDown();
                    String data = IntegrationHelper.emptySplitChanges(1000, 1000);
                    return createResponse(200, data);
                } else if (uri.getPath().contains("/auth")) {
                    System.out.println("AUTH HIT");
                    return createResponse(200, IntegrationHelper.streamingEnabledToken());
                } else {
                    return new HttpResponseMock(200);
                }
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    Logger.i("** SSE Connect hit");
                    mSseHitCount++;
                    mSseLatch.countDown();
                    return createStreamResponse(200, mStreamingData);
                } catch (Exception e) {
                }
                return null;
            }
        };
    }

    private String loadMockedData(String fileName) {
        FileHelper fileHelper = new FileHelper();
        return fileHelper.loadFileContent(mContext, fileName);
    }

    private void pushMessage(String fileName) {
        String message = loadMockedData(fileName);
        try {
            mStreamingData.put(message + "" + "\n");

            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }
}
