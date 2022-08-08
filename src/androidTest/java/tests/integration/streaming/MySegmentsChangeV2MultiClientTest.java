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
import fake.SynchronizerSpyImpl;
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestingData;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.logger.Logger;
import io.split.sharedtest.fake.HttpStreamResponseMock;
import tests.integration.shared.TestingHelper;

public class MySegmentsChangeV2MultiClientTest {
    private Context mContext;
    private BlockingQueue<String> mStreamingData;
    private CountDownLatch mMySegmentsUpdateLatch;
    private CountDownLatch mMySegmentsUpdateLatch2;
    private CountDownLatch mMySegmentsSyncLatch;
    private CountDownLatch mMySegmentsSyncLatch2;
    private CountDownLatch mSseLatch;
    private String mApiKey;

    final static String MSG_SEGMENT_UPDATE_TEMPLATE = "push_msg-segment_updV2.txt";

    private int mMySegmentsHitCount = 0;

    SplitFactory mFactory;
    SplitClient mClient;
    SynchronizerSpyImpl mSynchronizerSpy;

    @Before
    public void setup() {
        mStreamingData = new LinkedBlockingDeque<>();
        mSynchronizerSpy = new SynchronizerSpyImpl();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();

        Pair<String, String> apiKeyAndDb = IntegrationHelper.dummyApiKeyAndDb();
        mApiKey = apiKeyAndDb.first;
        mMySegmentsSyncLatch2 = new CountDownLatch(1);
        mMySegmentsUpdateLatch2 = new CountDownLatch(1);
    }

    @Test
    public void mySegmentsUpdateMultiClient() throws IOException, InterruptedException {
        SplitRoomDatabase db = DatabaseHelper.getTestDatabase(mContext);
        String userKey = "key1";
        String userKey2 = "key2";
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch readyLatch2 = new CountDownLatch(1);
        mSseLatch = new CountDownLatch(1);
        mMySegmentsSyncLatch = new CountDownLatch(2);
        mMySegmentsUpdateLatch = new CountDownLatch(1);

        TestingHelper.TestEventTask updateTask = new TestingHelper.TestEventTask();
        TestingHelper.TestEventTask updateTask2 = new TestingHelper.TestEventTask();

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher(2));

        SplitClientConfig config = IntegrationHelper.basicConfig();

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, new Key(userKey),
                config, mContext, httpClientMock, db, mSynchronizerSpy);

        mClient = mFactory.client();
        SplitClient client2 = mFactory.client(new Key(userKey2));

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);
        SplitEventTaskHelper readyTask2 = new SplitEventTaskHelper(readyLatch2);

        mClient.on(SplitEvent.SDK_READY, readyTask);
        mClient.on(SplitEvent.SDK_UPDATE, updateTask);
        client2.on(SplitEvent.SDK_READY, readyTask2);
        client2.on(SplitEvent.SDK_UPDATE, updateTask2);

        // Wait for SDK ready to fire
        readyLatch.await(10, TimeUnit.SECONDS);
        readyLatch2.await(10, TimeUnit.SECONDS);

        // Wait for streaming connection
        mSseLatch.await(15, TimeUnit.SECONDS);

        // Keep alive on streaming channel confirms connection
        // so full sync is fired
        TestingHelper.pushKeepAlive(mStreamingData);

        // Wait for hitting my segments two times (sdk ready and full sync after streaming connection)
        mMySegmentsSyncLatch.await(10, TimeUnit.SECONDS);
        mMySegmentsSyncLatch2.await(10, TimeUnit.SECONDS);

        // Unbounded fetch notification should trigger my segments
        // refresh on synchronizer
        // Set count to 0 to start counting hits
        mSynchronizerSpy.mForceMySegmentSyncCalledCount.set(0);

        CountDownLatch l1 = new CountDownLatch(1);
        CountDownLatch l2 = new CountDownLatch(2);
        updateTask.setLatch(l1);
        updateTask2.setLatch(l2);
        testMySegmentsUpdate(TestingData.UNBOUNDED_NOTIFICATION);
        l1.await(5, TimeUnit.SECONDS);
        l2.await(5, TimeUnit.SECONDS);

        // Should not trigger any fetch to my segments because
        // this payload doesn't have "key1" enabled
        pushMessage(TestingData.ESCAPED_BOUNDED_NOTIFICATION_GZIP);

        // Pushed key list message. Key 1 should add a segment
        l1 = new CountDownLatch(1);
        updateTask.setLatch(l1);
        pushMessage(TestingData.ESCAPED_KEY_LIST_NOTIFICATION_GZIP);
        l1.await(5, TimeUnit.SECONDS);

        l1 = new CountDownLatch(1);
        updateTask.setLatch(l1);
        pushMessage(TestingData.SEGMENT_REMOVAL_NOTIFICATION);
        l1.await(5, TimeUnit.SECONDS);

        MySegmentEntity mySegmentEntity = getByKey(userKey, db);
        MySegmentEntity mySegmentEntity2 = getByKey(userKey2, db);
        Assert.assertTrue(mySegmentEntity.getSegmentList().contains("new_segment_added"));
        Assert.assertFalse(mySegmentEntity.getSegmentList().contains("segment1"));

        Assert.assertEquals("new_segment_added", mySegmentEntity2.getSegmentList());

        mFactory.destroy();
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

    private HttpResponseMockDispatcher createBasicResponseDispatcher(int number) {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/mySegments/key2")) {
                    mMySegmentsSyncLatch2.countDown();
                    mMySegmentsUpdateLatch2.countDown();
                    return createResponse(200, IntegrationHelper.emptyMySegments());
                } else if (uri.getPath().contains("/mySegments")) {
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
                    BlockingQueue<String> queue = mStreamingData;
                    return createStreamResponse(200, queue);
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

    private void testMySegmentsUpdate(String msg) throws InterruptedException {
        mMySegmentsUpdateLatch = new CountDownLatch(1);
        pushMessage(msg);
        mMySegmentsUpdateLatch.await(5, TimeUnit.SECONDS);
    }

    private void pushMessage(String msg) {
        BlockingQueue<String> queue = mStreamingData;
        String message = loadMockedData(MSG_SEGMENT_UPDATE_TEMPLATE);
        message = message.replace("$TIMESTAMP$", String.valueOf(System.currentTimeMillis()));
        message = message.replace(TestingHelper.MSG_DATA_FIELD, msg);
        try {
            queue.put(message + "" + "\n");
            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }

    private String updatedMySegments(int count) {

        StringBuilder b = new StringBuilder();
        for (int i = 0; i < count; i++) {
            b.append("{ \"id\":\"id" + i + "\", \"name\":\"segment" + i + "\"},");
        }
        b.deleteCharAt(b.length() - 1); // Removing last ","

        return "{\"mySegments\":[" + b.toString() + "]}";

    }

    private MySegmentEntity getByKey(String userKey, SplitRoomDatabase db) {
        int count = 0;
        MySegmentEntity mySegmentEntity = null;
        while (mySegmentEntity == null && count < 10) {
            mySegmentEntity = db.mySegmentDao().getByUserKey(userKey);
            count++;
            System.out.println("Load count: " + count);
            try {
                sleep(200);
            } catch (Exception e){
            }
        }
        return mySegmentEntity;
    }

}
