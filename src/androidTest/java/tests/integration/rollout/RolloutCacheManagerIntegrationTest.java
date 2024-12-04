package tests.integration.rollout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static helper.IntegrationHelper.buildFactory;
import static helper.IntegrationHelper.dummyApiKey;
import static helper.IntegrationHelper.dummyUserKey;
import static helper.IntegrationHelper.emptySplitChanges;
import static helper.IntegrationHelper.getTimestampDaysAgo;
import static helper.IntegrationHelper.randomizedAllSegments;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import io.split.android.client.RolloutCacheConfiguration;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.MyLargeSegmentEntity;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.SplitLogLevel;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import tests.integration.shared.TestingHelper;

public class RolloutCacheManagerIntegrationTest {

    private final AtomicReference<String> mSinceFromUri = new AtomicReference<>(null);
    private MockWebServer mWebServer;
    private SplitRoomDatabase mRoomDb;
    private Context mContext = InstrumentationRegistry.getInstrumentation().getContext();

    @Before
    public void setUp() {
        mSinceFromUri.set(null);
        setupServer();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();
    }

    @Test
    public void expirationPeriodIsUsed() throws InterruptedException {


//
//        Verify persistent storage of flags & segments is cleared

        // Preload DB with update timestamp of 1 day ago
        long oldTimestamp = getTimestampDaysAgo(1);
        preloadDb(oldTimestamp, 0L, 18000L);

        // Track initial values
        List<SplitEntity> initialFlags = mRoomDb.splitDao().getAll();
        List<MySegmentEntity> initialSegments = mRoomDb.mySegmentDao().getAll();
        List<MyLargeSegmentEntity> initialLargeSegments = mRoomDb.myLargeSegmentDao().getAll();
        long initialChangeNumber = mRoomDb.generalInfoDao().getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO).getLongValue();

        // Initialize SDK with an expiration of 1 day
        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitFactory factory = getSplitFactory(RolloutCacheConfiguration.builder().expiration(1).build());

        // Track final values
        List<SplitEntity> finalFlags = mRoomDb.splitDao().getAll();
        List<MySegmentEntity> finalSegments = mRoomDb.mySegmentDao().getAll();
        List<MyLargeSegmentEntity> finalLargeSegments = mRoomDb.myLargeSegmentDao().getAll();
        long finalChangeNumber = mRoomDb.generalInfoDao().getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO).getLongValue();

        // Wait for ready
        factory.client().on(SplitEvent.SDK_READY, TestingHelper.testTask(readyLatch));
        boolean readyAwait = readyLatch.await(10, TimeUnit.SECONDS);

        assertTrue(readyAwait);
        assertEquals(2, initialFlags.size());
        assertEquals(1, initialSegments.size());
        assertEquals(1, initialLargeSegments.size());
        assertEquals(18000L, initialChangeNumber);
        assertEquals(0, finalFlags.size());
        assertEquals(0, finalSegments.size());
        assertEquals(0, finalLargeSegments.size());
        assertEquals(-1, finalChangeNumber);
        assertTrue(0L < mRoomDb.generalInfoDao()
                .getByName("rolloutCacheLastClearTimestamp").getLongValue());
        assertEquals("-1", mSinceFromUri.get());
    }

    private SplitFactory getSplitFactory(RolloutCacheConfiguration rolloutCacheConfiguration) {
        final String url = mWebServer.url("/").url().toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url).eventsEndpoint(url).build();
        SplitClientConfig.Builder builder = new SplitClientConfig.Builder()
                .serviceEndpoints(endpoints)
                .streamingEnabled(false)
                .featuresRefreshRate(9999)
                .segmentsRefreshRate(9999)
                .impressionsRefreshRate(9999)
                .logLevel(SplitLogLevel.VERBOSE)
                .streamingEnabled(false);

        if (rolloutCacheConfiguration != null) {
            builder.rolloutCacheConfiguration(rolloutCacheConfiguration);
        }

        SplitClientConfig config = builder
                .build();

        return buildFactory(
                dummyApiKey(), dummyUserKey(),
                config, mContext, null, mRoomDb);
    }

    private void setupServer() {
        mWebServer = new MockWebServer();

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                Thread.sleep(1000);
                if (request.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new MockResponse().setResponseCode(200).setBody(randomizedAllSegments());
                } else if (request.getPath().contains("/" + IntegrationHelper.ServicePath.SPLIT_CHANGES)) {
                    mSinceFromUri.compareAndSet(null, IntegrationHelper.getSinceFromUri(request.getRequestUrl().uri()));
                    return new MockResponse().setResponseCode(200)
                            .setBody(emptySplitChanges(-1, 10000));
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }

    private void preloadDb(long updateTimestamp, long lastClearTimestamp, long changeNumber) {
        List<Split> splitListFromJson = getSplitListFromJson();
        List<SplitEntity> entities = splitListFromJson.stream()
                .filter(split -> split.name != null)
                .map(split -> {
                    SplitEntity result = new SplitEntity();
                    result.setName(split.name);
                    result.setBody(Json.toJson(split));

                    return result;
                }).collect(Collectors.toList());

        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 1));
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, updateTimestamp));
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity("rolloutCacheLastClearTimestamp", lastClearTimestamp));
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, changeNumber));

        MyLargeSegmentEntity largeSegment = new MyLargeSegmentEntity();
        largeSegment.setSegmentList("{\"m1\":1,\"m2\":1}");
        largeSegment.setUserKey(dummyUserKey().matchingKey());
        largeSegment.setUpdatedAt(System.currentTimeMillis());
        mRoomDb.myLargeSegmentDao().update(largeSegment);

        MySegmentEntity segment = new MySegmentEntity();
        segment.setSegmentList("m1,m2");
        segment.setUserKey(dummyUserKey().matchingKey());
        segment.setUpdatedAt(System.currentTimeMillis());
        mRoomDb.mySegmentDao().update(segment);
        mRoomDb.splitDao().insert(entities);
    }

    private List<Split> getSplitListFromJson() {
        FileHelper fileHelper = new FileHelper();
        String s = fileHelper.loadFileContent(mContext, "attributes_test_split_change.json");

        SplitChange changes = Json.fromJson(s, SplitChange.class);

        return changes.splits;
    }
}
