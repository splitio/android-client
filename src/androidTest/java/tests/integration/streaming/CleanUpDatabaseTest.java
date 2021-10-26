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
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.storage.db.EventDao;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.ImpressionDao;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.utils.Logger;
import io.split.sharedtest.fake.HttpStreamResponseMock;

import static java.lang.Thread.sleep;

public class CleanUpDatabaseTest {
    Context mContext;
    BlockingQueue<String> mStreamingData;
    CountDownLatch mSplitsSyncLatch;
    CountDownLatch mMySegmentsSyncLatch;
    String mApiKey;
    Key mUserKey;

    EventDao mEventDao;
    ImpressionDao mImpressionDao;

    SplitFactory mFactory;
    SplitClient mClient;

    SplitRoomDatabase mSplitRoomDatabase;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mSplitsSyncLatch = new CountDownLatch(2);
        mMySegmentsSyncLatch = new CountDownLatch(2);

        Pair<String, String> apiKeyAndDb = IntegrationHelper.dummyApiKeyAndDb();
        mApiKey = apiKeyAndDb.first;
        String dataFolderName = apiKeyAndDb.second;
        mSplitRoomDatabase = DatabaseHelper.getTestDatabase(mContext);
        mEventDao = mSplitRoomDatabase.eventDao();
        mImpressionDao = mSplitRoomDatabase.impressionDao();
        mSplitRoomDatabase.clearAllTables();
        mUserKey = IntegrationHelper.dummyUserKey();
    }


    @Test
    public void testCleanUp() throws IOException, InterruptedException {

        mEventDao.insert(createEventEntity(now() - 1, StorageRecordStatus.DELETED, "deleted")); // mark as deleted
        mEventDao.insert(createEventEntity(now() + 10, StorageRecordStatus.ACTIVE, "active")); // active (shoud not be deleted)
        mEventDao.insert(createEventEntity(expiratedTime(), StorageRecordStatus.ACTIVE, "expirated")); // expirated

        mImpressionDao.insert(createImpressionEntity(now() - 1, StorageRecordStatus.DELETED, "deleted"));
        mImpressionDao.insert(createImpressionEntity(now() + 10, StorageRecordStatus.ACTIVE, "active"));
        mImpressionDao.insert(createImpressionEntity(expiratedTime(), StorageRecordStatus.ACTIVE, "expirated"));

        // Load records to check if inserted correctly on assert stage
        List<EventEntity> insertedEvents = mEventDao.getBy(0, StorageRecordStatus.ACTIVE, 10);
        insertedEvents.addAll(mEventDao.getBy(0, StorageRecordStatus.DELETED, 10));

        List<ImpressionEntity> insertedImpressions = mImpressionDao.getBy(0, StorageRecordStatus.ACTIVE, 10);
        insertedImpressions.addAll(mImpressionDao.getBy(0, StorageRecordStatus.DELETED, 10));

        CountDownLatch latch = new CountDownLatch(1);

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = SplitClientConfig.builder()
                .ready(30000)
                .streamingEnabled(true)
                .impressionsRefreshRate(999999999)
                .eventFlushInterval(99999999)
                .enableDebug()
                .trafficType("account")
                .build();

        mFactory = IntegrationHelper.buildFactory(
                mApiKey, IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock, mSplitRoomDatabase);

        mClient = mFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);

        mClient.on(SplitEvent.SDK_READY, readyTask);

        latch.await(40, TimeUnit.SECONDS);

        // wait to allow cleanup to run
        sleep(5000);

        // Load all records again after cleanup
        List<EventEntity> remainingEvents = mEventDao.getBy(0, StorageRecordStatus.ACTIVE, 10);
        remainingEvents.addAll(mEventDao.getBy(0, StorageRecordStatus.DELETED, 10));

        List<ImpressionEntity> remainingImpressions = mImpressionDao.getBy(0, StorageRecordStatus.ACTIVE, 10);
        remainingImpressions.addAll(mImpressionDao.getBy(0, StorageRecordStatus.DELETED, 10));

        Assert.assertEquals(3, insertedEvents.size());
        Assert.assertEquals(3, insertedImpressions.size());
        Assert.assertEquals(1, remainingEvents.size());
        Assert.assertEquals(1, remainingImpressions.size());

    }

    private EventEntity createEventEntity(long createdAt, int status, String name) {
        EventEntity entity = new EventEntity();
        entity.setBody("{" + name + "}");
        entity.setCreatedAt(createdAt);
        entity.setStatus(status);
        return entity;
    }

    private ImpressionEntity createImpressionEntity(long createdAt, int status, String name) {
        ImpressionEntity entity = new ImpressionEntity();
        entity.setBody("{" + name + "}");
        entity.setTestName(name);
        entity.setCreatedAt(createdAt);
        entity.setStatus(status);
        return entity;
    }

    private long now() {
        return System.currentTimeMillis() / 1000;
    }

    private long expiratedTime() {
        return now() - (ServiceConstants.RECORDED_DATA_EXPIRATION_PERIOD + 1000);
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
                    return createResponse(200, IntegrationHelper.dummyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    String data = IntegrationHelper.emptySplitChanges(-1, 1000);
                    return createResponse(200, data);
                } else if (uri.getPath().contains("/auth")) {
                    Logger.i("** SSE Auth hit");
                    return createResponse(200, IntegrationHelper.streamingEnabledToken());
                } else if (uri.getPath().contains("/bulk")) {
                    return createResponse(500, "");
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

    private String updatedMySegments() {
        return "{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, " +
                " { \"id\":\"id1\", \"name\":\"segment2\"}, " +
                "{ \"id\":\"id3\", \"name\":\"segment3\"}]}";
    }

}