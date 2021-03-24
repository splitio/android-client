package tests.integration;

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
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.sharedtest.fake.HttpStreamResponseMock;

import static java.lang.Thread.sleep;

public class SplitsTwoDifferentApiKeyTest {
    Context mContext;
    BlockingQueue<String> mStreamingData;

    SplitChange mSplitChange;

    final static String MSG_SPLIT_UPDATE = "push_msg-split_update.txt";
    final static String MSG_SPLIT_UPDATE_OLD = "push_msg-split_update_old_change_nb.txt";

    private int mSplitChangesHitCount = 0;

    SplitFactory mFactory1;
    SplitClient mClient1;
    final static String mApiKey1 = UUID.randomUUID().toString();// Random to avoid old data issues
    final static String mUserKey1 = "CUSTOMER_1";

    SplitFactory mFactory2;
    SplitClient mClient2;
    final static String mApiKey2 = UUID.randomUUID().toString(); // Random to avoid old data issues
    final static String mUserKey2 = "CUSTOMER_2";

    final static long CHANGE_NUMBER_BASE = 1000;
    final static long CHANGE_NUMBER_F1 = CHANGE_NUMBER_BASE + 1;
    final static long CHANGE_NUMBER_F2 = CHANGE_NUMBER_BASE + 2;
    long[] lastChangeNumbers = { 0, 0, 0};

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();

        loadSplitChanges();
    }


    @Test
    public void initialization() throws IOException, InterruptedException {

        // Factory 1 init
        CountDownLatch latch1 = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher(1));

        SplitClientConfig config = IntegrationHelper.basicConfig();

        mFactory1 = IntegrationHelper.buildFactory(
                mApiKey1, IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock);

        mClient1 = mFactory1.client();

        SplitEventTaskHelper readyTask1 = new SplitEventTaskHelper(latch1);

        mClient1.on(SplitEvent.SDK_READY, readyTask1);

        latch1.await(40, TimeUnit.SECONDS);

        String t1Split1 = mClient2.getTreatment("split1");
        String t1Split2 = mClient2.getTreatment("split2");

        /// Factory 2 init
        CountDownLatch latch2 = new CountDownLatch(1);

        HttpClientMock httpClientMock2 = new HttpClientMock(createBasicResponseDispatcher(2));


        mFactory2 = IntegrationHelper.buildFactory(
                mApiKey2, IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock);

        mClient2 = mFactory2.client();

        SplitEventTaskHelper readyTask2 = new SplitEventTaskHelper(latch2);

        mClient2.on(SplitEvent.SDK_READY, readyTask2);

        latch2.await(40, TimeUnit.SECONDS);

        String t2Split1 = mClient2.getTreatment("split1");
        String t2Split2 = mClient2.getTreatment("split2");


        // Assert factory 1
        Assert.assertEquals(t1Split1, "xxx");
        Assert.assertEquals(t1Split2, "control");
        Assert.assertEquals(CHANGE_NUMBER_F1, lastChangeNumbers[1]);

        // Assert factory 2
        Assert.assertEquals(t2Split1, "control");
        Assert.assertEquals(t2Split2, "xxxxx");
        Assert.assertEquals(CHANGE_NUMBER_F1, lastChangeNumbers[2]);

    }

    @After
    public void tearDown() {
        mFactory2.destroy();
    }

    private HttpResponseMock createResponse(int status, String data) {
        return new HttpResponseMock(status, data);
    }

    private HttpStreamResponseMock createStreamResponse(int status, BlockingQueue<String> streamingResponseData) throws IOException {
        return new HttpStreamResponseMock(status, streamingResponseData);
    }

    private HttpResponseMockDispatcher createBasicResponseDispatcher(int factoryNumber) {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/mySegments")) {

                    return createResponse(200, IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    long respChangeNumber = CHANGE_NUMBER_BASE + factoryNumber;
                    lastChangeNumbers[factoryNumber] = new Integer(uri.getQuery().split("=")[1]);
//                    Logger.i("** Split Changes hit p: " + mLastChangeNumber);
                    if (lastChangeNumbers[factoryNumber] == 1) {
                        return createResponse(200, getSplitChanges(factoryNumber));
                    }
                    String data = IntegrationHelper.emptySplitChanges(respChangeNumber, respChangeNumber);
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
                    Logger.i("** SSE Connect hit");
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

    private void loadSplitChanges() {
        mSplitChange = Json.fromJson(
                loadMockedData("splitchanges_int_test.json"), SplitChange.class);
    }

    private String getSplitChanges(int factoryNumber) {
        mSplitChange.splits.get(0).name = "SPLIT_" + factoryNumber;
        mSplitChange.since = CHANGE_NUMBER_BASE + factoryNumber;
        mSplitChange.till = CHANGE_NUMBER_BASE + factoryNumber;
        return Json.toJson(mSplitChange);
    }

    private Split parseEntity(SplitEntity entity) {
        return Json.fromJson(entity.getBody(), Split.class);
    }

}