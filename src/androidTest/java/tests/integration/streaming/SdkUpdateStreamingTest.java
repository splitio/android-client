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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Segment;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;
import fake.HttpStreamResponseMock;

import static java.lang.Thread.sleep;

public class SdkUpdateStreamingTest {
    Context mContext;
    BlockingQueue<String> mStreamingData;

    CountDownLatch mSseLatch;
    String mApiKey = IntegrationHelper.dummyApiKey();
    Key mUserKey = new Key("key1");

    final static String MSG_SPLIT_UPDATE = "push_msg-split_update.txt";
    final static String MSG_SPLIT_KILL = "push_msg-split_kill.txt";

    CountDownLatch mSplitsPushLatch = null;
    CountDownLatch mMySegmentsPushLatch = null;

    private int mSplitChangesHitCount = 0;
    private int mMySegmentsHitCount = 0;

    SplitFactory mFactory;
    SplitClient mClient;

    SplitRoomDatabase mSplitRoomDatabase;

    List<String> mTreatments;
    List<Long> mNumbers;
    List<String> mSplitChangesJson;

    @Before
    public void setup() {
        mTreatments = Arrays.asList("si", "si", "free", "conta", "no");
        mNumbers = Arrays.asList(500L, 1000L, 2000L, 3000L, 4000L);
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mSseLatch = new CountDownLatch(1);
        Pair<String, String> apiKeyAndDb = IntegrationHelper.dummyApiKeyAndDb();
        mSplitRoomDatabase = DatabaseHelper.getTestDatabase(mContext);
        mSplitRoomDatabase.clearAllTables();
        mSplitRoomDatabase.generalInfoDao().update(
                new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 99999));
        mSplitChangesJson = new ArrayList<>();
        loadChanges();
    }

    @Test
    public void readyTest() throws IOException, InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createNoChangesDispatcher());

        SplitClientConfig config = IntegrationHelper.basicConfig();
        mSplitRoomDatabase.generalInfoDao().update(
                new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 1000));

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, mUserKey,
                config, mContext, httpClientMock, mSplitRoomDatabase);

        mClient = mFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);
        SplitEventTaskHelper timeoutTask = new SplitEventTaskHelper(readyLatch);
        SplitEventTaskHelper updatedTask = new SplitEventTaskHelper();

        mClient.on(SplitEvent.SDK_READY, readyTask);
        mClient.on(SplitEvent.SDK_READY_TIMED_OUT, timeoutTask);
        mClient.on(SplitEvent.SDK_UPDATE, updatedTask);

        readyLatch.await(5, TimeUnit.SECONDS);
        mSseLatch.await(10, TimeUnit.SECONDS);
        pushInitialId();

        TestingHelper.delay(1000);

        Assert.assertTrue(readyTask.isOnPostExecutionCalled);
        Assert.assertFalse(updatedTask.isOnPostExecutionCalled);
    }

    @Test
    public void sdkUpdateSplitsWhenNotificationArrives() throws IOException, InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createSplitChangesDispatcher());

        SplitClientConfig config = IntegrationHelper.basicConfig();
        mSplitRoomDatabase.generalInfoDao().update(
                new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 1000));

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, mUserKey,
                config, mContext, httpClientMock, mSplitRoomDatabase);

        mClient = mFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);
        SplitEventTaskHelper timeoutTask = new SplitEventTaskHelper(readyLatch);
        SplitEventTaskHelper updatedTask = new SplitEventTaskHelper(updateLatch);

        mClient.on(SplitEvent.SDK_READY, readyTask);
        mClient.on(SplitEvent.SDK_READY_TIMED_OUT, timeoutTask);
        mClient.on(SplitEvent.SDK_UPDATE, updatedTask);

        readyLatch.await(5, TimeUnit.SECONDS);
        mSseLatch.await(10, TimeUnit.SECONDS);
        pushInitialId();

        testSplitsUpdate();
        updateLatch.await(10, TimeUnit.SECONDS);

        Assert.assertTrue(readyTask.isOnPostExecutionCalled);
        Assert.assertTrue(updatedTask.isOnPostExecutionCalled);
    }

    @Test
    public void sdkUpdateSplitsWhenKillNotificationArrives() throws IOException, InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createSplitChangesDispatcher());

        SplitClientConfig config = IntegrationHelper.basicConfig();
        mSplitRoomDatabase.generalInfoDao().update(
                new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 100));

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, mUserKey,
                config, mContext, httpClientMock, mSplitRoomDatabase);

        mClient = mFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);
        SplitEventTaskHelper timeoutTask = new SplitEventTaskHelper(readyLatch);
        SplitEventTaskHelper updatedTask = new SplitEventTaskHelper(updateLatch);

        mClient.on(SplitEvent.SDK_READY, readyTask);
        mClient.on(SplitEvent.SDK_READY_TIMED_OUT, timeoutTask);
        mClient.on(SplitEvent.SDK_UPDATE, updatedTask);

        readyLatch.await(5, TimeUnit.SECONDS);
        mSseLatch.await(10, TimeUnit.SECONDS);
        pushInitialId();

        testSplitKill();
        updateLatch.await(10, TimeUnit.SECONDS);

        Assert.assertTrue(readyTask.isOnPostExecutionCalled);
        Assert.assertTrue(updatedTask.isOnPostExecutionCalled);
    }

    @Test
    public void sdkUpdateMySegmentsWhenNotificationArrives() throws IOException, InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createMySegmentsChangesDispatcher());

        SplitClientConfig config = IntegrationHelper.basicConfig();
        mSplitRoomDatabase.generalInfoDao().update(
                new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 500));

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, new Key("key1"),
                config, mContext, httpClientMock, mSplitRoomDatabase);

        mClient = mFactory.client();

        TestingHelper.TestEventTask readyTask = TestingHelper.testTask(readyLatch);
        TestingHelper.TestEventTask timeoutTask = TestingHelper.testTask(readyLatch);
        TestingHelper.TestEventTask updatedTask = TestingHelper.testTask(updateLatch);

        mClient.on(SplitEvent.SDK_READY, readyTask);
        mClient.on(SplitEvent.SDK_READY_TIMED_OUT, timeoutTask);
        mClient.on(SplitEvent.SDK_UPDATE, updatedTask);

        readyLatch.await(20, TimeUnit.SECONDS);
        mSseLatch.await(20, TimeUnit.SECONDS);
        TestingHelper.pushKeepAlive(mStreamingData);

// TODO        testMySegmentsUpdate();
// TODO        updateLatch.await(20, TimeUnit.SECONDS);
// TODO
// TODO        Assert.assertTrue(readyTask.onExecutedCalled);
// TODO        Assert.assertTrue(updatedTask.onExecutedCalled);
    }

    private void testSplitKill() throws IOException, InterruptedException {
        mSplitsPushLatch = new CountDownLatch(1);
        pushMessage(MSG_SPLIT_KILL);
        mSplitsPushLatch.await(5, TimeUnit.SECONDS);
        mSplitsPushLatch = null;
    }

    private void testSplitsUpdate() throws IOException, InterruptedException {
        mSplitsPushLatch = new CountDownLatch(1);
        pushMessage(MSG_SPLIT_UPDATE);
        mSplitsPushLatch.await(5, TimeUnit.SECONDS);
        mSplitsPushLatch = null;
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

    private HttpResponseMockDispatcher createNoChangesDispatcher() {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                Logger.e("PATH IS " + uri.getPath());
                if (uri.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    Logger.i("NO CHANGWES MY S");
                    return createResponse(200, IntegrationHelper.dummyAllSegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    Logger.i("NO CHANGES changes");
                    return createResponse(200, IntegrationHelper.emptySplitChanges(99999, 99999));
                } else if (uri.getPath().contains("/auth")) {
                    Logger.i("** SSE Auth hit");
                    mSseLatch.countDown();
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

    private HttpResponseMockDispatcher createSplitChangesDispatcher() {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return createResponse(200, IntegrationHelper.dummyAllSegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    mSplitChangesHitCount++;
                    if(mSplitsPushLatch != null) {
                        mSplitsPushLatch.countDown();
                    }
                    return createResponse(200, getChanges(mSplitChangesHitCount));
                } else if (uri.getPath().contains("/auth")) {
                    mSseLatch.countDown();
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

    private HttpResponseMockDispatcher createMySegmentsChangesDispatcher() {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                Logger.e("PATH IS " + uri.getPath());
                if (uri.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    mMySegmentsHitCount++;
                    int hit = mMySegmentsHitCount;
                    String json = IntegrationHelper.emptyAllSegments();
                    if (mMySegmentsHitCount > 2) {
                        StringBuilder mySegments = new StringBuilder();
                        mySegments.append("{\"ms\":\"k\":[");
                        List<String> segmentList = new ArrayList<>();
                        for (int i = 0; i <= hit; i++) {
                            segmentList.add("{\"n\":\"" + "s" + i + "\"}");
                        }
                        mySegments.append(String.join(",", segmentList));
                        mySegments.append("],\"cn\":99999}");
                        json = "{\"ms\": " + mySegments + "}";
                    }
                    if(mMySegmentsPushLatch != null) {
                        mMySegmentsPushLatch.countDown();
                    }
                    return createResponse(200, json);
                } else if (uri.getPath().contains("/splitChanges")) {

                    return createResponse(200, IntegrationHelper.emptySplitChanges(mNumbers.get(0), mNumbers.get(0)));
                } else if (uri.getPath().contains("/auth")) {
                    return createResponse(200, IntegrationHelper.streamingEnabledToken());
                } else {
                    return new HttpResponseMock(200);
                }
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    Logger.i("** SSE Connect hit");
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

            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }

    private void pushInitialId() {
        try {
            mStreamingData.put("id:a62260de-13bb-11eb-adc1-0242ac120002" + "\n");
            Logger.d("Pushed initial ID");
        } catch (InterruptedException e) {
        }
    }

    private String getChanges(String treatment, long since, long till) {
        SplitChange change = Json.fromJson(
                loadMockedData("splitchanges_int_test.json"), SplitChange.class);
        change.since = since;
        change.till = till;
        Split split = change.splits.get(0);
        List<Partition> partitions = split.conditions.get(0).partitions;
        for (Partition partition : partitions) {
            if (partition.treatment.equals(treatment)) {
                partition.size = 100;
            } else {
                partition.size = 0;
            }
        }
        return Json.toJson(change);
    }

    private void loadChanges() {
        for (int i = 0; i < 5; i++) {
            String change = getChanges(mTreatments.get(i), mNumbers.get(i), mNumbers.get(i));
            mSplitChangesJson.add(change);
        }
    }

    private String getChanges(int hitNumber) {
        if (hitNumber < mNumbers.size()) {
            return mSplitChangesJson.get(hitNumber);
        }
        return IntegrationHelper.emptySplitChanges(500, 500);
    }

    private Split parseEntity(SplitEntity entity) {
        return Json.fromJson(entity.getBody(), Split.class);
    }

}
