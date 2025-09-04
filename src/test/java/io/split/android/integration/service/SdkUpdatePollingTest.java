package io.split.android.integration.service;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.AllSegmentsChange;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;
import io.split.android.helpers.FileHelper;
import io.split.sharedtest.fake.HttpClientMock;
import io.split.sharedtest.fake.HttpResponseMock;
import io.split.sharedtest.fake.HttpResponseMockDispatcher;
import io.split.sharedtest.fake.HttpStreamResponseMock;
import io.split.sharedtest.helper.DatabaseHelper;
import io.split.sharedtest.helper.IntegrationHelper;
import io.split.sharedtest.helper.SplitEventTaskHelper;
import io.split.sharedtest.helper.TestableSplitConfigBuilder;
import io.split.sharedtest.helper.TestingHelper;
import io.split.sharedtest.rules.WorkManagerRule;

@RunWith(RobolectricTestRunner.class)
public class SdkUpdatePollingTest {

    @Rule
    public WorkManagerRule mWorkManagerRule = new WorkManagerRule();

    Context mContext;
    BlockingQueue<String> mStreamingData;

    CountDownLatch mSseLatch;
    String mApiKey = IntegrationHelper.dummyApiKey();
    Key mUserKey = IntegrationHelper.dummyUserKey();

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
        mContext = ApplicationProvider.getApplicationContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mSseLatch = new CountDownLatch(1);
        mSplitRoomDatabase = DatabaseHelper.getTestDatabase(mContext);
        mSplitRoomDatabase.clearAllTables();
        mSplitRoomDatabase.generalInfoDao().update(
                new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 99999));
        mSplitChangesJson = new ArrayList<>();
        loadChanges();
    }

    @Test
    public void sdkUpdateSplits() throws IOException, InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createSplitChangesDispatcher());

        SplitClientConfig config = new TestableSplitConfigBuilder()
                .streamingEnabled(false)
                .featuresRefreshRate(1)
                .segmentsRefreshRate(1)
                .ready(3000)
                .build();
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
        mSseLatch.await(5, TimeUnit.SECONDS);

        updateLatch.await(5, TimeUnit.SECONDS);

        Assert.assertTrue(readyTask.isOnPostExecutionCalled);
        Assert.assertTrue(updatedTask.isOnPostExecutionCalled);
    }

    @Test
    public void sdkUpdateMySegments() throws IOException, InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createMySegmentsChangesDispatcher());

        SplitClientConfig config = new TestableSplitConfigBuilder()
                .streamingEnabled(false)
                .featuresRefreshRate(1)
                .segmentsRefreshRate(1)
                .ready(3000)
                .build();
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
        mSseLatch.await(5, TimeUnit.SECONDS);
        TestingHelper.pushKeepAlive(mStreamingData);

        testMySegmentsUpdate();
        updateLatch.await(5, TimeUnit.SECONDS);

        Assert.assertTrue(readyTask.isOnPostExecutionCalled);
        Assert.assertTrue(updatedTask.isOnPostExecutionCalled);
    }

    private void testSplitsUpdate() throws IOException, InterruptedException {

    }

    private void testMySegmentsUpdate() throws IOException, InterruptedException {

    }

    @After
    public void tearDown() {
        mFactory.destroy();
        if (mSplitRoomDatabase != null) {
            mSplitRoomDatabase.close();
        }
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
                if (uri.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    Logger.i("NO CHANGWES MY S");
                    return createResponse(200, IntegrationHelper.dummyAllSegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    // Bootstrap from -1 to a positive since/till, then echo since==till
                    String since = IntegrationHelper.getSinceFromUri(uri);
                    long s;
                    try {
                        s = Long.parseLong(since);
                    } catch (Exception e) {
                        s = -1L;
                    }
                    String json;
                    if (s < 0) {
                        json = IntegrationHelper.emptySplitChanges(-1, 100);
                    } else {
                        json = IntegrationHelper.emptySplitChanges(s, s);
                    }
                    Logger.i("NO CHANGES changes: " + json);
                    return createResponse(200, json);
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
                    String json = getChanges(mSplitChangesHitCount);
                    Logger.d("REPS CH: " + json);
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
                if (uri.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    mMySegmentsHitCount++;
                    int hit = mMySegmentsHitCount;
                    List<String> mySegments = new ArrayList<>();
                    for (int i = 0; i <= hit; i++) {
                        mySegments.add("segment" + i);
                    }
                    String json = Json.toJson(new AllSegmentsChange(mySegments));
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
        return new FileHelper().loadFileContent(fileName);
    }

    private String getChanges(String treatment, long since, long till) {
        SplitChange change = IntegrationHelper.getChangeFromJsonString(
                loadMockedData("splitchanges_int_test.json"));
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
        return Json.toJson(TargetingRulesChange.create(change));
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
        return IntegrationHelper.emptySplitChanges(5000, 5000);
    }

    private Split parseEntity(SplitEntity entity) {
        return Json.fromJson(entity.getBody(), Split.class);
    }

}
