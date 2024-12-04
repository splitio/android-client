package tests.integration.rollout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static helper.IntegrationHelper.buildFactory;
import static helper.IntegrationHelper.dummyApiKey;
import static helper.IntegrationHelper.dummyUserKey;
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
import io.split.android.client.dtos.SegmentsChange;
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
    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private CountDownLatch mRequestCountdownLatch;

    @Before
    public void setUp() {
        mSinceFromUri.set(null);
        setupServer();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();
        mRequestCountdownLatch = new CountDownLatch(1);
    }

    @Test
    public void expirationPeriodIsUsed() throws InterruptedException {
        test(getTimestampDaysAgo(1), RolloutCacheConfiguration.builder().expiration(1));
    }

    @Test
    public void clearOnInitClearsCacheOnStartup() throws InterruptedException {
        test(System.currentTimeMillis(), RolloutCacheConfiguration.builder().clearOnInit(true));
    }

    @Test
    public void repeatedInitWithClearOnInitSetToTrueDoesNotClearIfMinDaysHasNotElapsed() throws InterruptedException {
        // Preload DB with update timestamp of 1 day ago
        long oldTimestamp = System.currentTimeMillis();
        preloadDb(oldTimestamp, 0L, 8000L);

        // Track initial values
        List<SplitEntity> initialFlags = mRoomDb.splitDao().getAll();
        List<MySegmentEntity> initialSegments = mRoomDb.mySegmentDao().getAll();
        List<MyLargeSegmentEntity> initialLargeSegments = mRoomDb.myLargeSegmentDao().getAll();
        long initialChangeNumber = mRoomDb.generalInfoDao().getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO).getLongValue();

        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitFactory factory = getSplitFactory(RolloutCacheConfiguration.builder().clearOnInit(true).build());
        Thread.sleep(1000);

        // Track intermediate values
        List<SplitEntity> intermediateFlags = mRoomDb.splitDao().getAll();
        List<MySegmentEntity> intermediateSegments = mRoomDb.mySegmentDao().getAll();
        List<MyLargeSegmentEntity> intermediateLargeSegments = mRoomDb.myLargeSegmentDao().getAll();
        long intermediateChangeNumber = mRoomDb.generalInfoDao().getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO).getLongValue();

        // Resume server responses after tracking DB values
        mRequestCountdownLatch.countDown();

        // Wait for ready
        factory.client().on(SplitEvent.SDK_READY, TestingHelper.testTask(readyLatch));
        boolean readyAwait = readyLatch.await(10, TimeUnit.SECONDS);

        factory.destroy();
        mRequestCountdownLatch = new CountDownLatch(1);

        preloadDb(null, null, null);
        SplitFactory factory2 = getSplitFactory(RolloutCacheConfiguration.builder().clearOnInit(true).build());
        Thread.sleep(1000);

        // Track intermediate values
        List<SplitEntity> factory2Flags = mRoomDb.splitDao().getAll();
        List<MySegmentEntity> factory2Segments = mRoomDb.mySegmentDao().getAll();
        List<MyLargeSegmentEntity> factory2LargeSegments = mRoomDb.myLargeSegmentDao().getAll();
        long factory2ChangeNumber = mRoomDb.generalInfoDao().getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO).getLongValue();

        // initial values
        assertTrue(readyAwait);
        assertEquals(2, initialFlags.size());
        assertEquals(1, initialSegments.size());
        assertFalse(Json.fromJson(initialSegments.get(0).getSegmentList(), SegmentsChange.class).getSegments().isEmpty());
        assertFalse(Json.fromJson(initialLargeSegments.get(0).getSegmentList(), SegmentsChange.class).getSegments().isEmpty());
        assertEquals(8000L, initialChangeNumber);

        // values after clear
        assertEquals(1, intermediateSegments.size());
        assertTrue(Json.fromJson(intermediateSegments.get(0).getSegmentList(), SegmentsChange.class).getSegments().isEmpty());
        assertEquals(1, intermediateLargeSegments.size());
        assertEquals(0, intermediateFlags.size());
        assertTrue(Json.fromJson(intermediateLargeSegments.get(0).getSegmentList(), SegmentsChange.class).getSegments().isEmpty());
        assertEquals(-1, intermediateChangeNumber);

        // values after second init (values were reinserted into DB); no clear
        assertEquals(2, factory2Flags.size());
        assertEquals(1, factory2Segments.size());
        assertFalse(Json.fromJson(factory2Segments.get(0).getSegmentList(), SegmentsChange.class).getSegments().isEmpty());
        assertFalse(Json.fromJson(factory2LargeSegments.get(0).getSegmentList(), SegmentsChange.class).getSegments().isEmpty());
        assertEquals(10000L, factory2ChangeNumber);
        assertTrue(0L < mRoomDb.generalInfoDao()
                .getByName("rolloutCacheLastClearTimestamp").getLongValue());
    }

    private void test(long timestampDaysAgo, RolloutCacheConfiguration.Builder configBuilder) throws InterruptedException {
        // Preload DB with update timestamp of 1 day ago
        long oldTimestamp = timestampDaysAgo;
        preloadDb(oldTimestamp, 0L, 8000L);

        // Track initial values
        List<SplitEntity> initialFlags = mRoomDb.splitDao().getAll();
        List<MySegmentEntity> initialSegments = mRoomDb.mySegmentDao().getAll();
        List<MyLargeSegmentEntity> initialLargeSegments = mRoomDb.myLargeSegmentDao().getAll();
        long initialChangeNumber = mRoomDb.generalInfoDao().getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO).getLongValue();

        // Initialize SDK
        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitFactory factory = getSplitFactory(configBuilder.build());
        Thread.sleep(1000);

        // Track final values
        verify(factory, readyLatch, initialFlags, initialSegments, initialLargeSegments, initialChangeNumber);
    }

    private void verify(SplitFactory factory, CountDownLatch readyLatch, List<SplitEntity> initialFlags, List<MySegmentEntity> initialSegments, List<MyLargeSegmentEntity> initialLargeSegments, long initialChangeNumber) throws InterruptedException {
        // Track final values
        List<SplitEntity> finalFlags = mRoomDb.splitDao().getAll();
        List<MySegmentEntity> finalSegments = mRoomDb.mySegmentDao().getAll();
        List<MyLargeSegmentEntity> finalLargeSegments = mRoomDb.myLargeSegmentDao().getAll();
        long finalChangeNumber = mRoomDb.generalInfoDao().getByName(GeneralInfoEntity.CHANGE_NUMBER_INFO).getLongValue();

        // Resume server responses after tracking DB values
        mRequestCountdownLatch.countDown();

        // Wait for ready
        factory.client().on(SplitEvent.SDK_READY, TestingHelper.testTask(readyLatch));
        boolean readyAwait = readyLatch.await(10, TimeUnit.SECONDS);

        // Verify
        assertTrue(readyAwait);
        assertEquals(2, initialFlags.size());
        assertEquals(1, initialSegments.size());
        assertFalse(Json.fromJson(initialSegments.get(0).getSegmentList(), SegmentsChange.class).getSegments().isEmpty());
        assertFalse(Json.fromJson(initialLargeSegments.get(0).getSegmentList(), SegmentsChange.class).getSegments().isEmpty());
        assertEquals(8000L, initialChangeNumber);
        assertEquals(1, finalSegments.size());
        assertTrue(Json.fromJson(finalSegments.get(0).getSegmentList(), SegmentsChange.class).getSegments().isEmpty());
        assertEquals(0, finalFlags.size());
        assertEquals(1, finalLargeSegments.size());
        assertTrue(Json.fromJson(finalLargeSegments.get(0).getSegmentList(), SegmentsChange.class).getSegments().isEmpty());
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
                mRequestCountdownLatch.await();
                if (request.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new MockResponse().setResponseCode(200).setBody(randomizedAllSegments());
                } else if (request.getPath().contains("/" + IntegrationHelper.ServicePath.SPLIT_CHANGES)) {
                    mSinceFromUri.compareAndSet(null, IntegrationHelper.getSinceFromUri(request.getRequestUrl().uri()));
                    return new MockResponse().setResponseCode(200)
                            .setBody(IntegrationHelper.emptySplitChanges(-1, 10000L));
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }

    private void preloadDb(Long updateTimestamp, Long lastClearTimestamp, Long changeNumber) {
        List<Split> splitListFromJson = getSplitListFromJson();
        List<SplitEntity> entities = splitListFromJson.stream()
                .filter(split -> split.name != null)
                .map(split -> {
                    SplitEntity result = new SplitEntity();
                    result.setName(split.name);
                    result.setBody(Json.toJson(split));

                    return result;
                }).collect(Collectors.toList());

        if (updateTimestamp != null) {
            mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, updateTimestamp));
        }
        if (lastClearTimestamp != null) {
            mRoomDb.generalInfoDao().update(new GeneralInfoEntity("rolloutCacheLastClearTimestamp", lastClearTimestamp));
        }
        if (changeNumber != null) {
            mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, changeNumber));
        }

        MyLargeSegmentEntity largeSegment = new MyLargeSegmentEntity();
        largeSegment.setSegmentList("{\"k\":[{\"n\":\"ls1\"},{\"n\":\"ls2\"}],\"cn\":null}");
        largeSegment.setUserKey(dummyUserKey().matchingKey());
        largeSegment.setUpdatedAt(System.currentTimeMillis());
        mRoomDb.myLargeSegmentDao().update(largeSegment);

        MySegmentEntity segment = new MySegmentEntity();
        segment.setSegmentList("{\"k\":[{\"n\":\"s1\"},{\"n\":\"s2\"}],\"cn\":null}");
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
