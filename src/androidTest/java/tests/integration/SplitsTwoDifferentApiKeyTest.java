package tests.integration;

import static java.lang.Thread.sleep;

import static helper.IntegrationHelper.ResponseClosure.getSinceFromUri;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.TestingConfig;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;
import fake.HttpStreamResponseMock;
import tests.integration.shared.TestingHelper;

public class SplitsTwoDifferentApiKeyTest {
    Context mContext;
    List<BlockingQueue<String>> mStreamingData;

    SplitChange mSplitChange;

    final static String MSG_SPLIT_UPDATE = "push_msg-split_update-chgnum.txt";

    SplitFactory mFactory1;
    SplitClient mClient1;
    final static String mApiKey1 = UUID.randomUUID().toString();// Random to avoid old data issues

    SplitFactory mFactory2;
    SplitClient mClient2;
    final static String mApiKey2 = UUID.randomUUID().toString(); // Random to avoid old data issues

    final static long CHANGE_NUMBER_BASE = 1000;
    final static long CHANGE_NUMBER_F1 = CHANGE_NUMBER_BASE + 1;
    final static long CHANGE_NUMBER_F2 = CHANGE_NUMBER_BASE + 2;
    List<Long> f1ChangeNumbers;
    List<Long> f2ChangeNumbers;

    List<CountDownLatch> mSplitsUpdateLatch;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new ArrayList<>();
        mStreamingData.add(new LinkedBlockingDeque<>());
        mStreamingData.add(new LinkedBlockingDeque<>());
        f1ChangeNumbers = new ArrayList<>();
        f2ChangeNumbers = new ArrayList<>();
        mSplitsUpdateLatch = new ArrayList<>();
        loadSplitChanges();
    }

    @Test
    public void initialization() throws IOException, InterruptedException {

        // Factory 1 init
        CountDownLatch latch1 = new CountDownLatch(1);

        SplitRoomDatabase db = DatabaseHelper.getTestDatabase(mContext);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher(1));

        SplitClientConfig config = IntegrationHelper.basicConfig();

        TestingConfig testingConfig = IntegrationHelper.testingConfig(1);

        mFactory1 = IntegrationHelper.buildFactory(
                mApiKey1, IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock, db, null, testingConfig);

        mClient1 = mFactory1.client();

        SplitEventTaskHelper readyTask1 = new SplitEventTaskHelper(latch1);

        mClient1.on(SplitEvent.SDK_READY, readyTask1);

        latch1.await(10, TimeUnit.SECONDS);
        TestingHelper.pushKeepAlive(mStreamingData.get(0));

        String t1Split1 = mClient1.getTreatment("split1");
        String t1Split2 = mClient1.getTreatment("split2");

        testSplitsUpdate(CHANGE_NUMBER_F1 + 1, 1);

        TestingHelper.delay(200);

        mFactory1.destroy();

        /// Factory 2 init
        CountDownLatch latch2 = new CountDownLatch(1);

        HttpClientMock httpClientMock2 = new HttpClientMock(createBasicResponseDispatcher(2));

        mFactory2 = IntegrationHelper.buildFactory(
                mApiKey2, IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock2, DatabaseHelper.getTestDatabase(mContext),
                null, testingConfig);

        mClient2 = mFactory2.client();

        SplitEventTaskHelper readyTask2 = new SplitEventTaskHelper(latch2);

        mClient2.on(SplitEvent.SDK_READY, readyTask2);

        TestingHelper.pushKeepAlive(mStreamingData.get(1));

        latch2.await(10, TimeUnit.SECONDS);

        String t2Split1 = mClient2.getTreatment("split1");
        String t2Split2 = mClient2.getTreatment("split2");

        testSplitsUpdate(CHANGE_NUMBER_F2 + 1, 2);

        TestingHelper.delay(200);

        // Assert factory 1
        Assert.assertEquals("no", t1Split1);
        Assert.assertEquals("control", t1Split2);
        Assert.assertEquals(-1, f1ChangeNumbers.get(0).longValue());
        Assert.assertEquals(CHANGE_NUMBER_F1, f1ChangeNumbers.get(1).longValue());

        // Assert factory 2
        Assert.assertEquals("control", t2Split1);
        Assert.assertEquals("no", t2Split2);
        Assert.assertEquals(-1, f2ChangeNumbers.get(0).longValue());
        Assert.assertEquals(CHANGE_NUMBER_F2, f2ChangeNumbers.get(1).longValue());

        mFactory2.destroy();
    }

    private HttpResponseMock createResponse(int status, String data) {
        return new HttpResponseMock(status, data);
    }

    private HttpStreamResponseMock createStreamResponse(int status, BlockingQueue<String> streamingResponseData) throws IOException {
        return new HttpStreamResponseMock(status, streamingResponseData);
    }

    int mF1HitNumber = 0;
    int mF2HitNumber = 0;
    private HttpResponseMockDispatcher createBasicResponseDispatcher(int factoryNumber) {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/mySegments")) {

                    return createResponse(200, IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    int hit = 0;
                    long changeNumber = Long.parseLong(getSinceFromUri(uri));//new Integer(uri.getQuery().split("&")[1].split("=")[1]);
                    if (factoryNumber == 1) {
                        System.out.println("hit 1 cn: " + changeNumber);
                        f1ChangeNumbers.add(changeNumber);
                        hit = mF1HitNumber;
                        mF1HitNumber++;
                    } else {
                        f2ChangeNumbers.add(changeNumber);
                        hit = mF2HitNumber;
                        mF2HitNumber++;
                    }
                    long respChangeNumber = CHANGE_NUMBER_BASE + factoryNumber + hit;
                    if (changeNumber == -1) {
                        return createResponse(200, getSplitChanges(factoryNumber, hit));
                    }
                    String data = IntegrationHelper.emptySplitChanges(respChangeNumber, respChangeNumber);
                    CountDownLatch latch = mSplitsUpdateLatch.get(factoryNumber - 1);
                    if (latch != null) {
                        latch.countDown();
                    }
                    return createResponse(200, data);
                } else if (uri.getPath().contains("/auth")) {
                    return createResponse(200, IntegrationHelper.streamingEnabledToken());
                } else {
                    return new HttpResponseMock(200);
                }
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    return createStreamResponse(200, mStreamingData.get(factoryNumber - 1));
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

    private void loadSplitChanges() {
        mSplitChange = Json.fromJson(
                loadMockedData("splitchanges_int_test.json"), SplitChange.class);
    }

    private String getSplitChanges(int factoryNumber, int hitNumber) {
        mSplitChange.splits.get(0).name = "split" + factoryNumber;
        mSplitChange.since = (hitNumber == 0 ? -1 : CHANGE_NUMBER_BASE + factoryNumber);
        mSplitChange.till = CHANGE_NUMBER_BASE + factoryNumber;
        return Json.toJson(mSplitChange);
    }

    private void testSplitsUpdate(long changeNumber, int factoryNumber) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        mSplitsUpdateLatch.add(latch);
        pushMessage(MSG_SPLIT_UPDATE, changeNumber, factoryNumber);
        latch.await(10, TimeUnit.SECONDS);
        sleep(200);
    }

    private void pushMessage(String fileName, long newChangeNum, int factoryNumber) {
        String message = loadMockedData(fileName);
        message = message.replace("$TIMESTAMP$", String.valueOf(System.currentTimeMillis()))
                .replace("$CHANGE_NUMBER$", String.valueOf(newChangeNum));
        try {
            mStreamingData.get(factoryNumber - 1).put(message + "" + "\n");

            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }


}
