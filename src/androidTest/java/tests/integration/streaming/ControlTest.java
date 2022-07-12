package tests.integration.streaming;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
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
import helper.TestingHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.telemetry.model.streaming.StreamingStatusStreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;
import io.split.sharedtest.fake.HttpStreamResponseMock;

public class ControlTest {
    private Context mContext;
    private BlockingQueue<String> mStreamingData;
    private CountDownLatch mSseConnectedLatch;
    private String mApiKey;
    private Key mUserKey;
    private long mTimestamp = 100;

    private final static String CONTROL_TYPE_PLACEHOLDER = "$CONTROL_TYPE$";
    private final static String CONTROL_TIMESTAMP_PLACEHOLDER = "$TIMESTAMP$";
    private final static String MSG_SEGMENT_UPDATE_PAYLOAD = "push_msg-segment_update_payload_generic.txt";
    private final static String MSG_CONTROL = "push_msg-control.txt";

    private SplitFactory mFactory;
    private SplitClient mClient;

    private CountDownLatch mRequestClosedLatch;
    private CountDownLatch mPushLatch;

    private HttpStreamResponseMock mStreamingResponse;
    private int  mSseConnectionCount;

    SplitRoomDatabase db;
    String mSplitChange;

    @Before
    public void setup() {

        mSseConnectionCount = 0;
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mSplitChange = loadSplitChanges();
        mStreamingData = new LinkedBlockingDeque<>();
        mSseConnectedLatch = new CountDownLatch(1);

        Pair<String, String> apiKeyAndDb = IntegrationHelper.dummyApiKeyAndDb();
        mApiKey = apiKeyAndDb.first;

        mUserKey = IntegrationHelper.dummyUserKey();
    }

    @Test
    public void controlNotification() throws IOException, InterruptedException {

        SynchronizerSpyImpl synchronizerSpy = new SynchronizerSpyImpl();
        db = DatabaseHelper.getTestDatabase(mContext);
        db.clearAllTables();

        CountDownLatch readyLatch = new CountDownLatch(1);
        TestingHelper.TestEventTask updateTask = TestingHelper.testTask(new CountDownLatch(1), "CONTROL notif update task");

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.lowRefreshRateConfig(true, true);

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, mUserKey,
                config, mContext, httpClientMock, db, synchronizerSpy);

        mClient = mFactory.client();

        String splitName = "workm";

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);

        mClient.on(SplitEvent.SDK_READY, readyTask);
        mClient.on(SplitEvent.SDK_UPDATE, updateTask);
        readyLatch.await(20, TimeUnit.SECONDS);

        mSseConnectedLatch.await(20, TimeUnit.SECONDS);
        TestingHelper.pushKeepAlive(mStreamingData);

        String treatmentReady = mClient.getTreatment(splitName);

        // Pause streaming
        synchronizerSpy.startPeriodicFetchLatch = new CountDownLatch(1);
        pushControl("STREAMING_PAUSED");
        synchronizerSpy.startPeriodicFetchLatch.await(10, TimeUnit.SECONDS);

        pushMySegmentsUpdatePayload("new_segment");

        sleep(1000);

        String treatmentPaused = mClient.getTreatment(splitName);
        // Enable streaming, push a new my segments payload update and check data again
        synchronizerSpy.stopPeriodicFetchLatch = new CountDownLatch(1);
        pushControl("STREAMING_RESUMED");
        synchronizerSpy.stopPeriodicFetchLatch.await(10, TimeUnit.SECONDS);
        sleep(200);
        assertTrue(StorageFactory.getTelemetryStorage(true).popStreamingEvents().stream().anyMatch(event -> {
            if (event instanceof StreamingStatusStreamingEvent) {
                return event.getEventData().intValue() == 1;
            }
            return false;
        }));

        updateTask.mLatch = new CountDownLatch(1);
        pushMySegmentsUpdatePayload("new_segment");
        updateTask.mLatch.await(10, TimeUnit.SECONDS);

        String treatmentEnabled = mClient.getTreatment(splitName);

        //Enable streaming, push a new my segments payload update and check data again
        updateTask.mLatch = new CountDownLatch(1);
        pushControl("STREAMING_DISABLED");
        updateTask.mLatch.await(5, TimeUnit.SECONDS);
        pushMySegmentsUpdatePayload("new_segment");
        sleep(1000);

        TelemetryStorage telemetryStorage = StorageFactory.getTelemetryStorage(true);
        assertEquals(0, telemetryStorage.popTokenRefreshes());

        String treatmentDisabled = mClient.getTreatment(splitName);

        Assert.assertEquals("on", treatmentReady);
        Assert.assertEquals("on", treatmentPaused);
        Assert.assertEquals("free", treatmentEnabled);
        Assert.assertEquals("on", treatmentDisabled);
        Assert.assertTrue(mStreamingResponse.isClosed());
    }

    @Test
    public void streamResetControlNotification() throws IOException, InterruptedException {

        SplitRoomDatabase db = DatabaseHelper.getTestDatabase(mContext);
        db.clearAllTables();
        MySegmentEntity dummySegmentEntity = new MySegmentEntity();
        dummySegmentEntity.setUserKey(mUserKey.matchingKey());
        dummySegmentEntity.setSegmentList("dummy");

        CountDownLatch latch = new CountDownLatch(1);
        TestingHelper.TestEventTask testTask = TestingHelper.testTask(new CountDownLatch(1), "Control test Update task");

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.basicConfig();

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock, db);

        mClient = mFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);

        mClient.on(SplitEvent.SDK_READY, readyTask);
        latch.await(20, TimeUnit.SECONDS);

        mSseConnectedLatch.await(20, TimeUnit.SECONDS);
        TestingHelper.pushKeepAlive(mStreamingData);

        sleep(200);

        mSseConnectedLatch = new CountDownLatch(1);
        pushControl("STREAMING_RESET");
        mSseConnectedLatch.await(20, TimeUnit.SECONDS);

        Assert.assertEquals(2, mSseConnectionCount);
    }

    private void pushMySegmentsUpdatePayload(String segmentName) throws IOException, InterruptedException {
        mPushLatch = new CountDownLatch(1);
        pushMySegmentMessage(segmentName);
        mPushLatch.await(10, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        if (mFactory != null) {
            mFactory.destroy();
        }
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
                    return createResponse(200, IntegrationHelper.emptyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {

                    Logger.i("** Split Changes hit");
                    return createResponse(200, mSplitChange);
                } else if (uri.getPath().contains("/auth")) {
                    mSseConnectionCount++;
                    Logger.i("** SSE Auth hit: " + mSseConnectionCount);
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

    private String loadSplitChanges() {
        String jsonChange = new FileHelper().loadFileContent(mContext, "simple_split.json");
        SplitChange change = Json.fromJson(jsonChange, SplitChange.class);
        change.since = 500;
        change.till = 500;
        return Json.toJson(change);
    }

    private void pushMySegmentMessage(String segmentName) {
        String message = loadMockedData(MSG_SEGMENT_UPDATE_PAYLOAD);
        message = message.replace("[SEGMENT_NAME]", segmentName);
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                String message = loadMockedData(MSG_CONTROL);
                message = message.replace(CONTROL_TYPE_PLACEHOLDER, controlType);mTimestamp+=100;
                message = message.replace(CONTROL_TIMESTAMP_PLACEHOLDER, String.valueOf(mTimestamp));

                try {
                    mStreamingData.put(message + "" + "\n");

                    Logger.d("Pushed message: " + message);
                } catch (InterruptedException e) {
                }
            }
        }).start();
    }
}
