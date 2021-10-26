package tests.integration.streaming;

import android.content.Context;

import androidx.core.util.Pair;
import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
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
import fake.SynchronizerSpyImpl;
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestingData;
import helper.TestingHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.service.synchronizer.SynchronizerSpy;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Logger;
import io.split.sharedtest.fake.HttpStreamResponseMock;

import static java.lang.Thread.sleep;

public class MySegmentsChangeV2Test {
    Context mContext;
    BlockingQueue<String> mStreamingData;
    CountDownLatch mMySegmentsUpdateLatch;
    CountDownLatch mMySegmentsSyncLatch;
    CountDownLatch mSseLatch;
    String mApiKey;

    final static String MSG_SEGMENT_UPDATE_TEMPLATE = "push_msg-segment_updV2.txt";

    private int mMySegmentsHitCount = 0;

    SplitFactory mFactory;
    SplitClient mClient;
    SynchronizerSpyImpl mSynchronizerSpy;

    SplitRoomDatabase mSplitRoomDatabase;

    @Before
    public void setup() {
        mSynchronizerSpy = new SynchronizerSpyImpl();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();

        Pair<String, String> apiKeyAndDb = IntegrationHelper.dummyApiKeyAndDb();
        mApiKey = apiKeyAndDb.first;
        String dataFolderName = apiKeyAndDb.second;
        mSplitRoomDatabase = DatabaseHelper.getTestDatabase(mContext);
        mSplitRoomDatabase.clearAllTables();
    }

    @Test
    public void mySegmentsUpdate() throws IOException, InterruptedException {
        String userKey = "key1";
        CountDownLatch readyLatch = new CountDownLatch(1);
        mSseLatch = new CountDownLatch(1);
        mMySegmentsSyncLatch = new CountDownLatch(2);
        mMySegmentsUpdateLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(4);
        TestingHelper.TestEventTask updateTask = TestingHelper.testTask(updateLatch);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.basicConfig();

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, new Key(userKey),
                config, mContext, httpClientMock, mSplitRoomDatabase, mSynchronizerSpy);

        mClient = mFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);

        mClient.on(SplitEvent.SDK_READY, readyTask);
        mClient.on(SplitEvent.SDK_UPDATE, updateTask);

        // Wait for SDK ready to fire
        readyLatch.await(10, TimeUnit.SECONDS);

        // Wait for streaming connection
        mSseLatch.await(5, TimeUnit.SECONDS);

        // Keep alive on streaming channel confirms connection
        // so full sync is fired
        TestingHelper.pushKeepAlive(mStreamingData);

        // Wait for hitting my segments two times (sdk ready and full sync after streaming connection)
        mMySegmentsSyncLatch.await(5, TimeUnit.SECONDS);


        // Unbounded fetch notification should trigger my segments
        // refresh on synchronizer
        // Set count to 0 to start counting hits
        mSynchronizerSpy.mForceMySegmentSyncCalledCount.set(0);
        testMySegmentsUpdate(TestingData.UNBOUNDED_NOTIFICATION);

        // Should not trigger any fetch to my segments because
        // this payload doesn't have "key1" enabled
        pushMessage(TestingData.ESCAPED_BOUNDED_NOTIFICATION_GZIP);

        // Pushed key list message. Key 1 should add a segment
        pushMessage(TestingData.ESCAPED_KEY_LIST_NOTIFICATION_GZIP);

        pushMessage(TestingData.SEGMENT_REMOVAL_NOTIFICATION);

        updateLatch.await(20, TimeUnit.SECONDS);
        MySegmentEntity mySegmentEntity = mSplitRoomDatabase.mySegmentDao().getByUserKeys(userKey);

        Assert.assertEquals(1, mSynchronizerSpy.mForceMySegmentSyncCalledCount.get());
        Assert.assertTrue(mySegmentEntity.getSegmentList().contains("new_segment_added"));
        Assert.assertFalse(mySegmentEntity.getSegmentList().contains("segment1"));

    }

    @Test
    public void mySegmentsUpdateBounded() throws IOException, InterruptedException {
        String userKey = "603516ce-1243-400b-b919-0dce5d8aecfd";
        CountDownLatch readyLatch = new CountDownLatch(1);
        mSseLatch = new CountDownLatch(1);
        mMySegmentsSyncLatch = new CountDownLatch(2);
        mMySegmentsUpdateLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(4);
        TestingHelper.TestEventTask updateTask = TestingHelper.testTask(updateLatch);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.basicConfig();

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, new Key(userKey),
                config, mContext, httpClientMock, mSplitRoomDatabase, mSynchronizerSpy);

        mClient = mFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);

        mClient.on(SplitEvent.SDK_READY, readyTask);
        mClient.on(SplitEvent.SDK_UPDATE, updateTask);

        // Wait for SDK ready to fire
        readyLatch.await(10, TimeUnit.SECONDS);

        // Wait for streaming connection
        mSseLatch.await(5, TimeUnit.SECONDS);

        // Keep alive on streaming channel confirms connection
        // so full sync is fired
        TestingHelper.pushKeepAlive(mStreamingData);

        // Wait for hitting my segments two times (sdk ready and full sync after streaming connection)
        mMySegmentsSyncLatch.await(5, TimeUnit.SECONDS);


        // Unbounded fetch notification should trigger my segments
        // refresh on synchronizer
        // Set count to 0 to start counting hits
        mSynchronizerSpy.mForceMySegmentSyncCalledCount.set(0);
        testMySegmentsUpdate(TestingData.ESCAPED_BOUNDED_NOTIFICATION_GZIP);

        // ZLIB payload
        testMySegmentsUpdate(TestingData.ESCAPED_BOUNDED_NOTIFICATION_ZLIB);

        // This malformed payload should trigger unbounded
        testMySegmentsUpdate(TestingData.ESCAPED_BOUNDED_NOTIFICATION_MALFORMED);

        Assert.assertEquals(3, mSynchronizerSpy.mForceMySegmentSyncCalledCount.get());

    }


    @After
    public void tearDown() {
        mFactory.destroy();
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
                    mMySegmentsHitCount++;
                    Logger.i("** My segments hit: " + mMySegmentsHitCount);
                    mMySegmentsSyncLatch.countDown();
                    if (mMySegmentsHitCount == 3) {
                        mMySegmentsUpdateLatch.countDown();
                        Logger.d("updatedMySegments SEGMENTS");
                    }
                    return createResponse(200, updatedMySegments(mMySegmentsHitCount));
                } else if (uri.getPath().contains("/splitChanges")) {
                    Logger.i("** Split Changes hit");
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

    private void testMySegmentsUpdate(String msg) throws IOException, InterruptedException {
        mMySegmentsUpdateLatch = new CountDownLatch(1);
        pushMessage(msg);
        mMySegmentsUpdateLatch.await(5, TimeUnit.SECONDS);
    }

    private void pushMessage(String msg) {
        String message = loadMockedData(MSG_SEGMENT_UPDATE_TEMPLATE);
        message = message.replace("$TIMESTAMP$", String.valueOf(System.currentTimeMillis()));
        message = message.replace(TestingHelper.MSG_DATA_FIELD, msg);
        try {
            mStreamingData.put(message + "" + "\n");
            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }

    private String updatedMySegments(int count) {

        StringBuilder b = new StringBuilder();
        for (int i=0; i<count; i++) {
            b.append("{ \"id\":\"id" + i + "\", \"name\":\"segment" + i + "\"},");
        }
        b.deleteCharAt(b.length() - 1); // Removing last ","

        return "{\"mySegments\":[" + b.toString() + "]}";

    }

}