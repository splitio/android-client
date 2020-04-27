package integration.streaming;

import android.content.Context;

import androidx.core.util.Pair;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import fake.HttpStreamResponseMock;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class SyncProcessTest {
    Context mContext;
    BlockingQueue<String> mStreamingData;
    CountDownLatch mSplitsSyncLatch;
    CountDownLatch mMySegmentsSyncLatch;
    String mApiKey;

    CountDownLatch mSplitsUpdateLatch;
    CountDownLatch mMySegmentsUpdateLatch;

    final static String MSG_SEGMENT_UPDATE = "push_msg-segment_update.json";
    final static String MSG_SPLIT_UPDATE = "push_msg-split_update.json";
    final static String MSG_SPLIT_KILL = "push_msg-split_kill.json";

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mSplitsSyncLatch = new CountDownLatch(2);
        mMySegmentsSyncLatch = new CountDownLatch(2);

        Pair<String, String> apiKeyAndDb = IntegrationHelper.dummyApiKeyAndDb();
        mApiKey = apiKeyAndDb.first;
        String dataFolderName = apiKeyAndDb.second;
        SplitRoomDatabase splitRoomDatabase = SplitRoomDatabase.getDatabase(mContext, dataFolderName);
        splitRoomDatabase.clearAllTables();
    }

    @Test
    public void sdkReady() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch readyFromCacheLatch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.basicConfig();

        SplitFactory splitFactory = IntegrationHelper.buidFactory(
                mApiKey, IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock);

        SplitClient client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);

        latch.await(40, TimeUnit.SECONDS);
        mSplitsSyncLatch.await(40, TimeUnit.SECONDS);
        mMySegmentsSyncLatch.await(40, TimeUnit.SECONDS);


        mMySegmentsUpdateLatch = new CountDownLatch(1);
        pushMessage(MSG_SEGMENT_UPDATE);
        mMySegmentsSyncLatch.await(40, TimeUnit.SECONDS);


        Assert.assertTrue(client.isReady());
        Assert.assertTrue(splitFactory.isReady());
        Assert.assertTrue(readyTask.isOnPostExecutionCalled);

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
                    mMySegmentsSyncLatch.countDown();
                    return createResponse(200, IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    Logger.i("** Split Changes hit");
                    mSplitsSyncLatch.countDown();
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
                    return createStreamResponse(200, mStreamingData);
                } catch (Exception e) {
                }
                return null;
            }
        };
    }

    private String loadMockedMessage(String fileName) {
        FileHelper fileHelper = new FileHelper();
        return fileHelper.loadFileContent(mContext, fileName);
    }

    private void pushMessage(String fileName) {
        String message = loadMockedMessage(fileName);
        try {
            mStreamingData.put(message);
            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }
}