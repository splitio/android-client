package tests.integration.sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static helper.IntegrationHelper.ResponseClosure.getSinceFromUri;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFilter;
import io.split.android.client.SyncConfig;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class FlagSetsPollingTest {

    private final FileHelper fileHelper = new FileHelper();
    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private CountDownLatch hitsLatch;
    private CountDownLatch firstChangeLatch;
    private CountDownLatch secondChangeLatch;
    private CountDownLatch thirdChangeLatch;
    private SplitRoomDatabase mRoomDb;

    @Before
    public void setUp() throws Exception {
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();

        hitsLatch = new CountDownLatch(3);
        firstChangeLatch = new CountDownLatch(1);
        secondChangeLatch = new CountDownLatch(1);
        thirdChangeLatch = new CountDownLatch(1);
    }

    @Test
    public void featureFlagIsUpdatedAccordingToSetsWhenTheyAreConfigured() throws IOException, InterruptedException {
        /*
        This test creates a factory with 2 configured sets.

        The first split change will have 2 splits (workm and workm_set_3), one that belongs to set_1 and set_2 and one that belongs to set_3;
            -> it should be added to storage

        The second change will have 1 split (workm) that belongs to set_1 only.
            -> it should remain in storage and be updated

        The third change will have 1 split (workm) that belongs to set_3 only.
            -> it should be removed from storage
         */

        createFactory(mContext, mRoomDb, "set_1", "set_2");

        boolean awaitFirst = firstChangeLatch.await(5, TimeUnit.SECONDS);
        Thread.sleep(200);
        int firstSize = mRoomDb.splitDao().getAll().size();
        boolean firstSetsCorrect = mRoomDb.splitDao().getAll().get(0).getBody().contains("[\"set_1\",\"set_2\"]");

        boolean awaitSecond = secondChangeLatch.await(5, TimeUnit.SECONDS);
        Thread.sleep(200);
        int secondSize = mRoomDb.splitDao().getAll().size();
        boolean secondSetsCorrect = mRoomDb.splitDao().getAll().get(0).getBody().contains("[\"set_1\"]");

        boolean awaitThird = thirdChangeLatch.await(5, TimeUnit.SECONDS);
        Thread.sleep(200);
        int thirdSize = mRoomDb.splitDao().getAll().size();

        boolean awaitHits = hitsLatch.await(120, TimeUnit.SECONDS);

        assertEquals(1, firstSize);
        assertEquals(1, secondSize);
        assertEquals(0, thirdSize);
        assertTrue(awaitFirst);
        assertTrue(awaitSecond);
        assertTrue(awaitThird);
        assertTrue(firstSetsCorrect);
        assertTrue(secondSetsCorrect);

        assertTrue(awaitHits);
    }

    @Test
    public void featureFlagSetsAreIgnoredWhenSetsAreNotConfigured() throws IOException, InterruptedException {
        /*
        This test creates a factory with no sets configured.

        The first split change will have 2 splits (workm and workm_set_3), one that belongs to set_1 and set_2 and one that belongs to set_3;
            -> both should be added to storage.

        The second change will have 1 split (workm) that belongs to set_1 only.
            -> that split should be updated.

        The third change will have 1 split (workm) that belongs to set_3 only.
            -> that split should be updated.
         */

        createFactory(mContext, mRoomDb);

        boolean awaitFirst = firstChangeLatch.await(5, TimeUnit.SECONDS);
        Thread.sleep(500);
        int firstSize = mRoomDb.splitDao().getAll().size();
        List<SplitEntity> firstEntities = mRoomDb.splitDao().getAll();
        boolean firstSetsCorrect = firstEntities.get(0).getBody().contains("[\"set_1\",\"set_2\"]") &&
                firstEntities.get(1).getBody().contains("[\"set_3\"]");

        boolean awaitSecond = secondChangeLatch.await(5, TimeUnit.SECONDS);
        Thread.sleep(500);
        int secondSize = mRoomDb.splitDao().getAll().size();
        List<SplitEntity> secondEntities = mRoomDb.splitDao().getAll();
        String body0 = secondEntities.get(0).getBody();
        String body1 = secondEntities.get(1).getBody();
        boolean secondSetsCorrect = body1.contains("[\"set_1\"]") &&
                body1.contains("\"name\":\"workm\",") &&
                body0.contains("\"name\":\"workm_set_3\",") &&
                body0.contains("[\"set_3\"]");

        Logger.w("body0: " + body0);
        Logger.w("body1: " + body1);

        boolean awaitThird = thirdChangeLatch.await(5, TimeUnit.SECONDS);
        Thread.sleep(500);
        List<SplitEntity> thirdEntities = mRoomDb.splitDao().getAll();
        int thirdSize = thirdEntities.size();
        String body30 = thirdEntities.get(0).getBody();
        String body31 = thirdEntities.get(1).getBody();
        boolean thirdSetsCorrect = body31.contains("[\"set_3\"]") &&
                body31.contains("\"name\":\"workm\",") &&
                body30.contains("\"name\":\"workm_set_3\",") &&
                body30.contains("[\"set_3\"]");

        boolean awaitHits = hitsLatch.await(120, TimeUnit.SECONDS);

        assertEquals(2, firstSize);
        assertEquals(2, secondSize);
        assertEquals(2, thirdSize);
        assertTrue(awaitFirst);
        assertTrue(awaitSecond);
        assertTrue(awaitThird);
        assertTrue(firstSetsCorrect);
        assertTrue(secondSetsCorrect);
        assertTrue(thirdSetsCorrect);

        assertTrue(awaitHits);
    }

    private SplitFactory createFactory(
            Context mContext,
            SplitRoomDatabase splitRoomDatabase,
            String... sets) throws IOException {
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .ready(30000)
                .trafficType("client")
                .enableDebug()
                .impressionsRefreshRate(1000)
                .impressionsCountersRefreshRate(1000)
                .syncConfig(SyncConfig.builder()
                        .addSplitFilter(SplitFilter.bySet(Arrays.asList(sets)))
                        .build())
                .featuresRefreshRate(2)
                .streamingEnabled(false)
                .eventFlushInterval(1000)
                .build();

        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put("splitChanges", (uri, httpMethod, body) -> {
            String since = getSinceFromUri(uri);

            hitsLatch.countDown();
            if (since.equals("-1")) {
                firstChangeLatch.countDown();
                return new HttpResponseMock(200, loadSplitChangeWithSet(2));
            } else if (since.equals("1602796638344")) {
                secondChangeLatch.countDown();
                return new HttpResponseMock(200, loadSplitChangeWithSet(1));
            } else {
                thirdChangeLatch.countDown();
                return new HttpResponseMock(200, loadSplitChangeWithSet(0));
            }
        });

        responses.put("mySegments/CUSTOMER_ID", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));

        HttpResponseMockDispatcher httpResponseMockDispatcher = IntegrationHelper.buildDispatcher(responses);

        return IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                config,
                mContext,
                new HttpClientMock(httpResponseMockDispatcher),
                splitRoomDatabase, null, null, null);
    }

    private String loadSplitChangeWithSet(int setsCount) {
        String change = fileHelper.loadFileContent(mContext, "split_changes_flag_set-" + setsCount + ".json");
        SplitChange parsedChange = Json.fromJson(change, SplitChange.class);
        parsedChange.since = parsedChange.till;

        return Json.toJson(parsedChange);
    }
}
