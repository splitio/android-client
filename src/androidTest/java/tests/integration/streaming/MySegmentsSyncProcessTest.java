package tests.integration.streaming;

import static java.lang.Thread.sleep;

import android.content.Context;

import androidx.core.util.Pair;
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
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.logger.Logger;
import fake.HttpStreamResponseMock;
import tests.integration.shared.TestingHelper;

public class MySegmentsSyncProcessTest {
    private Context mContext;
    private BlockingQueue<String> mStreamingData;
    private CountDownLatch mMySegmentsSyncLatch;
    private CountDownLatch mSseLatch;
    private String mApiKey;
    private Key mUserKey;

    private CountDownLatch mMySegmentsUpdateLatch;
    private CountDownLatch mMySegmentsPushLatch;

    private  final static String MSG_SEGMENT_UPDATE = "push_msg-segment_update.txt";
    private final static String MSG_SEGMENT_UPDATE_PAYLOAD = "push_msg-segment_update_payload.txt";
    private final static String MSG_SEGMENT_UPDATE_EMPTY_PAYLOAD = "push_msg-segment_update_empty_payload.txt";

    private int mMySegmentsHitCount = 0;

    SplitFactory mFactory;
    SplitClient mClient;

    SplitRoomDatabase mSplitRoomDatabase;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mMySegmentsSyncLatch = new CountDownLatch(2);
        mSseLatch = new CountDownLatch(1);

        Pair<String, String> apiKeyAndDb = IntegrationHelper.dummyApiKeyAndDb();
        mApiKey = apiKeyAndDb.first;
        mSplitRoomDatabase = DatabaseHelper.getTestDatabase(mContext);
        mSplitRoomDatabase.clearAllTables();
        mUserKey = new Key("key1");
        mMySegmentsHitCount = 0;
    }

    @Test
    public void mySegmentsUpdate() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.basicConfig();

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, mUserKey,
                config, mContext, httpClientMock, mSplitRoomDatabase);

        mClient = mFactory.client();

        TestingHelper.TestEventTask readyTask = new TestingHelper.TestEventTask(latch);


        CountDownLatch updLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask updTask = new TestingHelper.TestEventTask(updLatch);

        mClient.on(SplitEvent.SDK_READY, readyTask);
        mClient.on(SplitEvent.SDK_UPDATE, updTask);

        latch.await(10, TimeUnit.SECONDS);

        mSseLatch.await(20, TimeUnit.SECONDS);

        TestingHelper.pushKeepAlive(mStreamingData);
        mMySegmentsSyncLatch.await(10, TimeUnit.SECONDS);

        updLatch = new CountDownLatch(1);
        updTask.setLatch(updLatch);
        testMySegmentsUpdate();
//        sleep(500);
        updLatch.await(5, TimeUnit.SECONDS);
        MySegmentEntity mySegmentEntity = mSplitRoomDatabase.mySegmentDao().getByUserKey(mUserKey.matchingKey());

        updLatch = new CountDownLatch(1);
        updTask.setLatch(updLatch);
        testMySegmentsPush(MSG_SEGMENT_UPDATE_PAYLOAD);
        updLatch.await(5, TimeUnit.SECONDS);
//        sleep(500);
        MySegmentEntity mySegmentEntityPayload = mSplitRoomDatabase.mySegmentDao().getByUserKey(mUserKey.matchingKey());

        testMySegmentsPush(MSG_SEGMENT_UPDATE_EMPTY_PAYLOAD);
        sleep(1000);
        MySegmentEntity mySegmentEntityEmptyPayload = mSplitRoomDatabase.mySegmentDao().getByUserKey(mUserKey.matchingKey());

        Assert.assertEquals("segment1,segment2,segment3", mySegmentEntity.getSegmentList());
        Assert.assertEquals("segment1", mySegmentEntityPayload.getSegmentList());
        Assert.assertEquals("", mySegmentEntityEmptyPayload.getSegmentList());
    }

    @Test
    public void multiClientSegmentsUpdateOnlyOneClient() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        mSseLatch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.basicConfig();

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, mUserKey,
                config, mContext, httpClientMock, mSplitRoomDatabase);

        mClient = mFactory.client();
        SplitClient client2 = mFactory.client(new Key("key2"));

        TestingHelper.TestEventTask readyTask = new TestingHelper.TestEventTask(latch);
        TestingHelper.TestEventTask readyTask2 = new TestingHelper.TestEventTask(latch2);

        TestingHelper.TestEventTask updTask = new TestingHelper.TestEventTask();

        mClient.on(SplitEvent.SDK_READY, readyTask);
        client2.on(SplitEvent.SDK_READY, readyTask2);

        mClient.on(SplitEvent.SDK_UPDATE, updTask);

        latch.await(10, TimeUnit.SECONDS);
        latch2.await(10, TimeUnit.SECONDS);

        mSseLatch.await(20, TimeUnit.SECONDS);

        TestingHelper.pushKeepAlive(mStreamingData);
        mMySegmentsSyncLatch.await(10, TimeUnit.SECONDS);

        CountDownLatch updLatch = new CountDownLatch(1);
        updTask.setLatch(updLatch);
        testMySegmentsUpdate();
        updLatch.await(5, TimeUnit.SECONDS);
//        sleep(1000);
        MySegmentEntity mySegmentEntity = mSplitRoomDatabase.mySegmentDao().getByUserKey(mUserKey.matchingKey());
        MySegmentEntity mySegmentEntity2 = mSplitRoomDatabase.mySegmentDao().getByUserKey("key2");

        updLatch = new CountDownLatch(1);
        updTask.setLatch(updLatch);
        testMySegmentsPush(MSG_SEGMENT_UPDATE_PAYLOAD);
        updLatch.await(5, TimeUnit.SECONDS);
//        sleep(1000);
        MySegmentEntity mySegmentEntityPayload = mSplitRoomDatabase.mySegmentDao().getByUserKey(mUserKey.matchingKey());
        MySegmentEntity mySegmentEntityPayload2 = mSplitRoomDatabase.mySegmentDao().getByUserKey("key2");

        testMySegmentsPush(MSG_SEGMENT_UPDATE_EMPTY_PAYLOAD);
        sleep(1000);
        MySegmentEntity mySegmentEntityEmptyPayload = mSplitRoomDatabase.mySegmentDao().getByUserKey(mUserKey.matchingKey());
        MySegmentEntity mySegmentEntityEmptyPayload2 = mSplitRoomDatabase.mySegmentDao().getByUserKey("key2");

        Assert.assertEquals("segment1,segment2,segment3", mySegmentEntity.getSegmentList());
        Assert.assertEquals("segment1", mySegmentEntityPayload.getSegmentList());
        Assert.assertEquals("", mySegmentEntityEmptyPayload.getSegmentList());

        Assert.assertEquals("", mySegmentEntity2.getSegmentList());
        Assert.assertEquals("", mySegmentEntityPayload2.getSegmentList());
        Assert.assertEquals("", mySegmentEntityEmptyPayload2.getSegmentList());
    }

    private void testMySegmentsUpdate() throws InterruptedException {
        mMySegmentsUpdateLatch = new CountDownLatch(1);
        pushMessage(MSG_SEGMENT_UPDATE);
        boolean await = mMySegmentsUpdateLatch.await(25, TimeUnit.SECONDS);
        if (!await) {
            Assert.fail("MySegments update not received");
        }
    }

    private void testMySegmentsPush(String message) throws InterruptedException {
        mMySegmentsPushLatch = new CountDownLatch(1);
        pushMessage(message);
        boolean await = mMySegmentsPushLatch.await(15, TimeUnit.SECONDS);
        if (!await) {
            Assert.fail("MySegments push not received");
        }
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
                if (uri.getPath().contains("auth")) {
                    return createResponse(200, IntegrationHelper.streamingEnabledV1Token());
                } else if (uri.getPath().contains("/mySegments/key1")) {
                    mMySegmentsHitCount++;
                    Logger.i("** My segments hit: " + mMySegmentsHitCount);
                    mMySegmentsSyncLatch.countDown();

                    if (mMySegmentsHitCount == 3) {
                        mMySegmentsUpdateLatch.countDown();
                        Logger.d("updatedMySegments SEGMENTS");
                        return createResponse(200, updatedMySegments());
                    }
                    Logger.d("DUMMY SEGMENTS");
                    return createResponse(200, IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/mySegments/key2")) {
                    return createResponse(200, IntegrationHelper.emptyMySegments());
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
                    e.printStackTrace();
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
        message = message.replace("$TIMESTAMP$", String.valueOf(System.currentTimeMillis()));
        try {
            mStreamingData.put(message + "" + "\n");
            if(mMySegmentsPushLatch != null) {
                mMySegmentsPushLatch.countDown();
            }
            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }

    private String updatedMySegments() {
        return "{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, " +
                " { \"id\":\"id1\", \"name\":\"segment2\"}, " +
                "{ \"id\":\"id3\", \"name\":\"segment3\"}]}";
    }

}
