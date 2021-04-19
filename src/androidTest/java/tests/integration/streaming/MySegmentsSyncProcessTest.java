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
import helper.TestingHelper;
import io.split.sharedtest.fake.HttpStreamResponseMock;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Logger;

import static java.lang.Thread.sleep;

public class MySegmentsSyncProcessTest {
    Context mContext;
    BlockingQueue<String> mStreamingData;
    CountDownLatch mMySegmentsSyncLatch;
    CountDownLatch mSseLatch;
    String mApiKey;
    Key mUserKey;

    CountDownLatch mMySegmentsUpdateLatch;
    CountDownLatch mMySegmentsPushLatch;

    final static String MSG_SEGMENT_UPDATE = "push_msg-segment_update.txt";
    final static String MSG_SEGMENT_UPDATE_PAYLOAD = "push_msg-segment_update_payload.txt";
    final static String MSG_SEGMENT_UPDATE_EMPTY_PAYLOAD = "push_msg-segment_update_empty_payload.txt";

    private int mMySegmentsHitCount = 0;

    SplitFactory mFactory;
    SplitClient mClient;

    SplitRoomDatabase mSplitRoomDatabase;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mMySegmentsSyncLatch = new CountDownLatch(2);

        Pair<String, String> apiKeyAndDb = IntegrationHelper.dummyApiKeyAndDb();
        mApiKey = apiKeyAndDb.first;
        String dataFolderName = apiKeyAndDb.second;
        mSplitRoomDatabase = Room.inMemoryDatabaseBuilder(mContext, SplitRoomDatabase.class).build();
        mSplitRoomDatabase.clearAllTables();
        mUserKey = IntegrationHelper.dummyUserKey();
    }


    @Test
    public void mySegmentsUpdate() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        mSseLatch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.basicConfig();

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock, mSplitRoomDatabase);

        mClient = mFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);

        mClient.on(SplitEvent.SDK_READY, readyTask);

        latch.await(10, TimeUnit.SECONDS);

        mSseLatch.await(5, TimeUnit.SECONDS);

        TestingHelper.pushKeepAlive(mStreamingData);
        mMySegmentsSyncLatch.await(10, TimeUnit.SECONDS);

        testMySegmentsUpdate();
        sleep(500);
        MySegmentEntity mySegmentEntity = mSplitRoomDatabase.mySegmentDao().getByUserKeys(mUserKey.matchingKey());

        testMySegmentsPush(MSG_SEGMENT_UPDATE_PAYLOAD);
        sleep(500);
        MySegmentEntity mySegmentEntityPayload = mSplitRoomDatabase.mySegmentDao().getByUserKeys(mUserKey.matchingKey());

        testMySegmentsPush(MSG_SEGMENT_UPDATE_EMPTY_PAYLOAD);
        sleep(1000);
        MySegmentEntity mySegmentEntityEmptyPayload = mSplitRoomDatabase.mySegmentDao().getByUserKeys(mUserKey.matchingKey());

        Assert.assertEquals("segment1,segment2,segment3", mySegmentEntity.getSegmentList());
        Assert.assertEquals("segment1", mySegmentEntityPayload.getSegmentList());
        Assert.assertEquals("", mySegmentEntityEmptyPayload.getSegmentList());

    }


    private void testMySegmentsUpdate() throws IOException, InterruptedException {
        mMySegmentsUpdateLatch = new CountDownLatch(1);
        pushMessage(MSG_SEGMENT_UPDATE);
        mMySegmentsUpdateLatch.await(5, TimeUnit.SECONDS);
    }

    private void testMySegmentsPush(String message) throws IOException, InterruptedException {
        mMySegmentsPushLatch = new CountDownLatch(1);
        pushMessage(message);
        mMySegmentsPushLatch.await(5, TimeUnit.SECONDS);
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
                        return createResponse(200, updatedMySegments());
                    }
                    Logger.d("DUMMY SEGMENTS");
                    return createResponse(200, IntegrationHelper.dummyMySegments());
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