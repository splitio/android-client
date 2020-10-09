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

public class ControlTest {
    private Context mContext;
    private BlockingQueue<String> mStreamingData;
    private CountDownLatch mSseConnectedLatch;
    private String mApiKey;
    private Key mUserKey;
    private long mTimestamp = 100;

    private CountDownLatch mMySegmentsUpdateLatch;

    private final static String CONTROL_TYPE_PLACEHOLDER = "$CONTROL_TYPE$";
    private final static String CONTROL_TIMESTAMP_PLACEHOLDER = "$TIMESTAMP$";
    private final static String MSG_SEGMENT_UPDATE_PAYLOAD = "push_msg-segment_update_payload.txt";
    private final static String MSG_CONTROL = "push_msg-control.txt";

    private SplitFactory mFactory;
    private SplitClient mClient;

    private CountDownLatch mRequestClosedLatch;
    private CountDownLatch mPushLatch;

    private SplitRoomDatabase mSplitRoomDatabase;

    private HttpStreamResponseMock mStreamingResponse;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mSseConnectedLatch = new CountDownLatch(1);

        Pair<String, String> apiKeyAndDb = IntegrationHelper.dummyApiKeyAndDb();
        mApiKey = apiKeyAndDb.first;
        String dataFolderName = apiKeyAndDb.second;
        mSplitRoomDatabase = Room.inMemoryDatabaseBuilder(mContext,
                SplitRoomDatabase.class)
                .build();
        mSplitRoomDatabase.clearAllTables();
        mUserKey = IntegrationHelper.dummyUserKey();
    }


    @Test
    public void controlt() throws IOException, InterruptedException {

        MySegmentEntity dummySegmenteEntity = new MySegmentEntity();
        dummySegmenteEntity.setUserKey(mUserKey.matchingKey());
        dummySegmenteEntity.setSegmentList("dummy");

        CountDownLatch latch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.basicConfig();

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock, mSplitRoomDatabase);

        mClient = mFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);

        mClient.on(SplitEvent.SDK_READY, readyTask);
        latch.await(5, TimeUnit.SECONDS);
        mSseConnectedLatch.await(5, TimeUnit.SECONDS);

        MySegmentEntity mySegmentEntityReady = mSplitRoomDatabase.mySegmentDao().getByUserKeys(mUserKey.matchingKey());

        // Update segments to test initial data ok
        pushMySegmentsUpdatePayload();
        sleep(2000);
        MySegmentEntity mySegmentEntityOne = mSplitRoomDatabase.mySegmentDao().getByUserKeys(mUserKey.matchingKey());

        // Remove data, pause streaming and then retrieve segments to assert that no one is available
        mSplitRoomDatabase.mySegmentDao().update(dummySegmenteEntity);
        /// Pause streaming
        pushControl("STREAMING_PAUSED");
        sleep(1000);
        pushMySegmentsUpdatePayload();
        sleep(1000);
        MySegmentEntity mySegmentEntityNone = mSplitRoomDatabase.mySegmentDao().getByUserKeys(mUserKey.matchingKey());

        // Enable streaming, push a new my segments payload update and check data again
        pushControl("STREAMING_ENABLED");
        sleep(2000);

        mSplitRoomDatabase.mySegmentDao().update(dummySegmenteEntity);
        pushMySegmentsUpdatePayload();
        sleep(2000);
        MySegmentEntity mySegmentEntityPayload = mSplitRoomDatabase.mySegmentDao().getByUserKeys(mUserKey.matchingKey());


        //Enable streaming, push a new my segments payload update and check data again
        pushControl("STREAMING_DISABLED");

        mRequestClosedLatch.await(5, TimeUnit.SECONDS);

        Assert.assertEquals("segment_ready", mySegmentEntityReady.getSegmentList());
        Assert.assertEquals("segment1", mySegmentEntityOne.getSegmentList());
        Assert.assertEquals("dummy", mySegmentEntityNone.getSegmentList());
        Assert.assertEquals("segment1", mySegmentEntityPayload.getSegmentList());
        Assert.assertTrue(mStreamingResponse.isClosed());
    }

    private void pushMySegmentsUpdatePayload() throws IOException, InterruptedException {
        mPushLatch = new CountDownLatch(1);
        pushMessage(MSG_SEGMENT_UPDATE_PAYLOAD);
        mPushLatch.await(5, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        mFactory.destroy();
    }

    private HttpResponseMock createResponse(int status, String data) {
        return new HttpResponseMock(status, data);
    }

    private HttpStreamResponseMock createStreamResponse(int status, BlockingQueue<String> streamingResponseData) throws IOException {
        mRequestClosedLatch = new CountDownLatch(1);
        mStreamingResponse = new HttpStreamResponseMock(status, streamingResponseData);
        mStreamingResponse.setClosedLatch(mRequestClosedLatch);
        return mStreamingResponse;
    }

    private HttpResponseMockDispatcher createBasicResponseDispatcher() {
        return new HttpResponseMockDispatcher() {
          int hit = 0;

            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/mySegments")) {
                    Logger.i("** My segments hit");
                    if (hit < 1) {
                        hit++;
                        return createResponse(200, "{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment_ready\"}]}");
                    } else {
                        // This is to avoid having issues for polling update while asserting
                        return createResponse(200, "{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}]}");
                    }
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
                    Logger.i("** SSE Connect hit");
                    mSseConnectedLatch.countDown();
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
        mTimestamp+=100;
        message = message.replace(CONTROL_TIMESTAMP_PLACEHOLDER, String.valueOf(mTimestamp));
        try {
            mStreamingData.put(message + "" + "\n");
            sleep(200);
            mPushLatch.countDown();
            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }

    private void pushControl(String controlType) {
        String message = loadMockedData(MSG_CONTROL);
        message = message.replace(CONTROL_TYPE_PLACEHOLDER, controlType);
        mTimestamp+=100;
        message = message.replace(CONTROL_TIMESTAMP_PLACEHOLDER, String.valueOf(mTimestamp));

        try {
            mStreamingData.put(message + "" + "\n");

            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }

}