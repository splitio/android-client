package tests.integration.streaming;

import android.content.Context;

import androidx.core.util.Pair;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import fake.HttpStreamResponseMock;
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.AllSegmentsChange;
import io.split.android.client.dtos.SegmentsChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;
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

    private final static String MSG_SEGMENT_UPDATE = "push_msg-segment_update.txt";
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
        CountDownLatch sdkReadyLatch = new CountDownLatch(1);
        CountDownLatch firstUpdateLatch = new CountDownLatch(1);
        CountDownLatch secondUpdateLatch = new CountDownLatch(1);
        CountDownLatch thirdUpdateLatch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());
        SplitClientConfig config = IntegrationHelper.basicConfig();

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, mUserKey,
                config, mContext, httpClientMock, mSplitRoomDatabase);

        mClient = mFactory.client();

        TestingHelper.TestEventTask readyTask = new TestingHelper.TestEventTask(sdkReadyLatch);
        TestingHelper.TestEventTask firstUpdateTask = new TestingHelper.TestEventTask(firstUpdateLatch);
        TestingHelper.TestEventTask secondUpdateTask = new TestingHelper.TestEventTask(secondUpdateLatch);
        TestingHelper.TestEventTask thirdUpdateTask = new TestingHelper.TestEventTask(thirdUpdateLatch);

        mClient.on(SplitEvent.SDK_READY, readyTask);
        mClient.on(SplitEvent.SDK_UPDATE, firstUpdateTask);

        sdkReadyLatch.await(10, TimeUnit.SECONDS);
        mSseLatch.await(20, TimeUnit.SECONDS);

        TestingHelper.pushKeepAlive(mStreamingData);
        mMySegmentsSyncLatch.await(10, TimeUnit.SECONDS);

        testMySegmentsUpdate();
        firstUpdateLatch.await(5, TimeUnit.SECONDS);
        MySegmentEntity mySegmentEntity = mSplitRoomDatabase.mySegmentDao().getByUserKey(mUserKey.matchingKey());

        mClient.on(SplitEvent.SDK_UPDATE, secondUpdateTask);
        testMySegmentsPush(MSG_SEGMENT_UPDATE_PAYLOAD);
        secondUpdateLatch.await(5, TimeUnit.SECONDS);
        MySegmentEntity mySegmentEntityPayload = mSplitRoomDatabase.mySegmentDao().getByUserKey(mUserKey.matchingKey());

        mClient.on(SplitEvent.SDK_UPDATE, thirdUpdateTask);
        testMySegmentsPush(MSG_SEGMENT_UPDATE_EMPTY_PAYLOAD);
        thirdUpdateLatch.await(5, TimeUnit.SECONDS);
        MySegmentEntity mySegmentEntityEmptyPayload = mSplitRoomDatabase.mySegmentDao().getByUserKey(mUserKey.matchingKey());

        Assert.assertTrue(mySegmentEntity.getSegmentList().contains("segment1") && mySegmentEntity.getSegmentList().contains("segment2") && mySegmentEntity.getSegmentList().contains("segment3"));
        String body = mySegmentEntityPayload.getSegmentList();
        SegmentsChange segmentsChange = Json.fromJson(body, SegmentsChange.class);
        Assert.assertEquals(Arrays.asList("segment1"), segmentsChange.getNames());
        Assert.assertEquals(1584647532812L, segmentsChange.getChangeNumber().longValue());
        Assert.assertEquals("{\"cn\":1584647532812,\"k\":[]}", mySegmentEntityEmptyPayload.getSegmentList());
    }

    @Test
    public void multiClientSegmentsUpdateOnlyOneClient() throws IOException, InterruptedException {
        CountDownLatch client1ReadyLatch = new CountDownLatch(1);
        CountDownLatch client2ReadyLatch = new CountDownLatch(1);
        mSseLatch = new CountDownLatch(1);
        final CountDownLatch client1UpdateLatch = new CountDownLatch(3);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());
        SplitClientConfig config = IntegrationHelper.basicConfig();

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, mUserKey,
                config, mContext, httpClientMock, mSplitRoomDatabase);

        mClient = mFactory.client();
        SplitClient client2 = mFactory.client(new Key("key2"));

        SplitEventTask client1ReadyTask = new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                client1ReadyLatch.countDown();
            }
        };

        SplitEventTask client2ReadyTask = new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                client2ReadyLatch.countDown();
            }
        };

        SplitEventTask client1UpdateTask = new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                client1UpdateLatch.countDown();
            }
        };

        mClient.on(SplitEvent.SDK_READY, client1ReadyTask);
        client2.on(SplitEvent.SDK_READY, client2ReadyTask);
        mClient.on(SplitEvent.SDK_UPDATE, client1UpdateTask);

        client1ReadyLatch.await(10, TimeUnit.SECONDS);
        client2ReadyLatch.await(10, TimeUnit.SECONDS);

        mSseLatch.await(20, TimeUnit.SECONDS);

        TestingHelper.pushKeepAlive(mStreamingData);
        mMySegmentsSyncLatch.await(10, TimeUnit.SECONDS);

        testMySegmentsUpdate();
        client1UpdateLatch.await(5, TimeUnit.SECONDS);
        MySegmentEntity client1SegmentEntity = mSplitRoomDatabase.mySegmentDao().getByUserKey(mUserKey.matchingKey());
        MySegmentEntity client2SegmentEntity = mSplitRoomDatabase.mySegmentDao().getByUserKey("key2");

        testMySegmentsPush(MSG_SEGMENT_UPDATE_PAYLOAD);
        client1UpdateLatch.await(5, TimeUnit.SECONDS);
        MySegmentEntity client1SegmentEntityPayload = mSplitRoomDatabase.mySegmentDao().getByUserKey(mUserKey.matchingKey());
        MySegmentEntity client2SegmentEntityPayload = mSplitRoomDatabase.mySegmentDao().getByUserKey("key2");

        testMySegmentsPush(MSG_SEGMENT_UPDATE_EMPTY_PAYLOAD);
        client1UpdateLatch.await(5, TimeUnit.SECONDS);
        MySegmentEntity client1SegmentEntityEmptyPayload = mSplitRoomDatabase.mySegmentDao().getByUserKey(mUserKey.matchingKey());
        MySegmentEntity client2SegmentEntityEmptyPayload = mSplitRoomDatabase.mySegmentDao().getByUserKey("key2");

        Assert.assertTrue(client1SegmentEntity.getSegmentList().contains("segment1") && client1SegmentEntity.getSegmentList().contains("segment2") && client1SegmentEntity.getSegmentList().contains("segment3"));
        Assert.assertEquals("{\"cn\":1584647532812,\"k\":[{\"n\":\"segment1\"}]}", client1SegmentEntityPayload.getSegmentList());
        Assert.assertEquals("{\"cn\":1584647532812,\"k\":[]}", client1SegmentEntityEmptyPayload.getSegmentList());

        Assert.assertEquals("{\"cn\":null,\"k\":[]}", client2SegmentEntity.getSegmentList());
        Assert.assertEquals("{\"cn\":null,\"k\":[]}", client2SegmentEntityPayload.getSegmentList());
        Assert.assertEquals("{\"cn\":null,\"k\":[]}", client2SegmentEntityEmptyPayload.getSegmentList());
    }

    private void testMySegmentsUpdate() throws InterruptedException {
        mMySegmentsUpdateLatch = new CountDownLatch(1);
        pushMessage(MSG_SEGMENT_UPDATE);
        boolean await = mMySegmentsUpdateLatch.await(30, TimeUnit.SECONDS);
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
                try {
                    Thread.sleep(800);
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
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
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
        new Thread(() -> {
            try {
                Thread.sleep(500);
                String message = loadMockedData(fileName);
                message = message.replace("$TIMESTAMP$", String.valueOf(System.currentTimeMillis()));
                mStreamingData.put(message + "" + "\n");
                if (mMySegmentsPushLatch != null) {
                    mMySegmentsPushLatch.countDown();
                }
                Logger.d("Pushed message: " + message);

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private String updatedMySegments() {
        return Json.toJson(new AllSegmentsChange(Arrays.asList("segment1", "segment2", "segment3")));
    }

}
