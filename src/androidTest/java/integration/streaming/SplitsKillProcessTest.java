package integration.streaming;

import android.content.Context;

import androidx.core.util.Pair;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import fake.HttpStreamResponseMock;
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

import static java.lang.Thread.sleep;

public class SplitsKillProcessTest {
    Context mContext;
    BlockingQueue<String> mStreamingData;
    CountDownLatch mSplitsSyncLatch;
    CountDownLatch mMySegmentsSyncLatch;
    String mApiKey;
    Key mUserKey;
    SplitChange mSplitChange;

    CountDownLatch mSplitsUpdateLatch;

    final static String MSG_SPLIT_KILL = "push_msg-split_kill.txt";
    final static String MSG_SPLIT_KILL_OLD = "push_msg-split_kill_old.txt";

    private int mSplitChangesHitCount = 0;

    SplitFactory mFactory;
    SplitClient mClient;

    SplitRoomDatabase mSplitRoomDatabase;

    final static long CHANGE_NUMBER = 1000200;

    private long mLastChangeNumber = 0;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mSplitsSyncLatch = new CountDownLatch(2);
        mMySegmentsSyncLatch = new CountDownLatch(2);

        Pair<String, String> apiKeyAndDb = IntegrationHelper.dummyApiKeyAndDb();
        mApiKey = apiKeyAndDb.first;
        String dataFolderName = apiKeyAndDb.second;
        mSplitRoomDatabase = SplitRoomDatabase.getDatabase(mContext, dataFolderName);
        mSplitRoomDatabase.clearAllTables();
        mUserKey = IntegrationHelper.dummyUserKey();
        mSplitRoomDatabase.generalInfoDao().update(
                new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, System.currentTimeMillis() / 1000 - 30));
        loadSplitChanges();
    }


    @Test
    public void updateTest() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = IntegrationHelper.basicConfig();

        mFactory = IntegrationHelper.buidFactory(
                mApiKey, IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock);

        mClient = mFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);

        mClient.on(SplitEvent.SDK_READY, readyTask);

        latch.await(40, TimeUnit.SECONDS);
        mSplitsSyncLatch.await(40, TimeUnit.SECONDS);
        mMySegmentsSyncLatch.await(40, TimeUnit.SECONDS);

        testSplitsKill();
        sleep(500);
        int splitCount =  mSplitRoomDatabase.splitDao().getAll().size();
        Split split = getSplitEntityByName(mSplitRoomDatabase.splitDao().getAll(), "test_feature");

        testOldSplitsKill();

        Split splitOld = getSplitEntityByName(mSplitRoomDatabase.splitDao().getAll(), "test_feature_1");
        sleep(500);
        long storedChangeNumber = mSplitRoomDatabase.generalInfoDao().getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO).getLongValue();

        Assert.assertEquals(2, splitCount);
        Assert.assertEquals(CHANGE_NUMBER, split.changeNumber);
        Assert.assertEquals("off", split.defaultTreatment);
        Assert.assertTrue(split.killed);
        Assert.assertFalse(splitOld.killed);
        Assert.assertEquals("yes", splitOld.defaultTreatment);
        Assert.assertEquals(3, mSplitChangesHitCount);
        Assert.assertEquals(CHANGE_NUMBER, storedChangeNumber);
        Assert.assertEquals(CHANGE_NUMBER - 1000, mLastChangeNumber);
    }

    private void testSplitsKill() throws IOException, InterruptedException {
        mSplitsUpdateLatch = new CountDownLatch(1);
        pushMessage(MSG_SPLIT_KILL);
        mSplitsUpdateLatch.await(40, TimeUnit.SECONDS);
    }

    private void testOldSplitsKill() throws IOException, InterruptedException {
        pushMessage(MSG_SPLIT_KILL_OLD);
    }

    private void testSplitKill() throws IOException, InterruptedException {
        mSplitsUpdateLatch = new CountDownLatch(1);
        pushMessage(MSG_SPLIT_KILL);
        mSplitsUpdateLatch.await(40, TimeUnit.SECONDS);
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
                    Logger.i("** My segments hit");
                    mMySegmentsSyncLatch.countDown();

                    return createResponse(200, IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {

                    mSplitChangesHitCount++;
                    mLastChangeNumber = new Integer(uri.getQuery().split("=")[1]);
                    Logger.i("** Split Changes hit p: " + mLastChangeNumber);
                    mSplitsSyncLatch.countDown();
                    if (mSplitChangesHitCount > 2) {
                        mSplitsUpdateLatch.countDown();
                        return createResponse(200, getSplitChanges(mSplitChangesHitCount));
                    }
                    String data = IntegrationHelper.emptySplitChanges(-1, CHANGE_NUMBER - 1000);
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
        try {
            mStreamingData.put(message + "" + "\n");

            Logger.d("Pushed message: " + message);
        } catch (InterruptedException e) {
        }
    }

    private void loadSplitChanges() {
        SplitChange change = Json.fromJson(
                loadMockedData("splitchanges_int_test.json"), SplitChange.class);

        Split split = change.splits.get(0);
        split.name = "test_feature_1";
        mSplitChange = Json.fromJson(
                loadMockedData("splitchanges_int_test.json"), SplitChange.class);
        mSplitChange.splits.add(split);
    }

    private String getSplitChanges(int hit) {

        if(hit > 3) {
            Split split = getSplitByName(mSplitChange.splits, "test_feature_1");
            split.killed = true;
        }

        Split split = getSplitByName(mSplitChange.splits, "test_feature");
        split.changeNumber = CHANGE_NUMBER;
        split.killed = true;
        split.defaultTreatment = "off";
        mSplitChange.till = CHANGE_NUMBER;
        return Json.toJson(mSplitChange);
    }

    private Split parseEntity(SplitEntity entity) {
        return Json.fromJson(entity.getBody(), Split.class);
    }

    private Split getSplitEntityByName(List<SplitEntity> splits, String name) {
        for(SplitEntity split : splits) {
            if(split.getName().equals(name)) {
                return parseEntity(split);
            }
        }
        return null;
    }

    private Split getSplitByName(List<Split> splits, String name) {
        for(Split split : splits) {
            if(split.name.equals(name)) {
                return split;
            }
        }
        return null;
    }
}