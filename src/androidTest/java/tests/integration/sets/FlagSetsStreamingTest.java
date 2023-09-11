package tests.integration.sets;

import static org.junit.Assert.assertNotNull;

import static helper.IntegrationHelper.ResponseClosure.getSinceFromUri;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
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
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFilter;
import io.split.android.client.SyncConfig;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;

public class FlagSetsStreamingTest {

    private final FileHelper fileHelper = new FileHelper();
    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private SplitRoomDatabase mRoomDb;

    @Before
    public void setUp() {
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();
    }

    @Test
    public void sdkWithoutSetsConfiguredDoesNotTakeFeatureFlagSetsIntoAccount() throws IOException, InterruptedException {
        SplitClient readyClient = getReadyClient(mContext, mRoomDb, true);

        assertNotNull(readyClient);


    }

    /**
     * SDK initialization with config.sets=["a", "b"] :
     * <p>
     * if a SPLIT_UPDATE is received with {name:"test", "sets":["a", "b"]}, it should process it since is part of the config.Sets
     * <p>
     * if a SPLIT_UPDATE is received with {name:"test", "sets":["a"]}, it should process it since is still part of the config.Sets
     * <p>
     * if a SPLIT_UPDATE is received with {name:"test", "sets":[]} and the featureFlag is present in the storage, means that it was part of the config.Sets but not anymore. The featureFlag should be removed.
     * <p>
     * if a SPLIT_UPDATE is received with {name:"test", "sets":["x"]} and the featureFlag is present in the storage, that means that was part of the config.Sets but not anymore. The featureFlag should be removed.
     * <p>
     * if a SPLIT_UPDATE is received with {name:"test", "sets":["x", "y"]}, and the featureFlag is not part of the storage, the notification should be discarded since is NOT part of the config.Sets
     * <p>
     * if a SPLIT_KILL is received with {cn:2, name:"test", "defaultTreatment":"off"} , two scenarios possibles:
     * <p>
     * if featureFlag is present in the storage, the featureFlag should process the local kill behaviour.
     * <p>
     * if not, a fetch must be needed.
     */

    @Test
    public void sdkWithSetsConfiguredDeletedDueToEmptySets() {
        /*
         * Initialize a factory with a & b sets configured.
         *
         * 1. Receive a SPLIT_UPDATE with {name:"test", "sets":["a", "b"]}. It should process it since is part of the config.Sets
         *
         * 2. Receive a SPLIT_UPDATE with {name:"test", "sets":["a"]}. It should process it since is still part of the config.Sets
         *
         * 3. Receive a SPLIT_UPDATE with {name:"test", "sets":[]}. The featureFlag should be removed.
         *
         */
    }

    @Test
    public void sdkWithSetsConfiguredDeletedDueToNonMatchingSets() {
        /*
         * Initialize a factory with a & b sets configured.
         *
         * 1. Receive a SPLIT_UPDATE with {name:"test", "sets":["a", "b"]}. It should process it since is part of the config.Sets
         *
         * 2. Receive a SPLIT_UPDATE with {name:"test", "sets":["x"]}. The featureFlag should be removed.
         *
         * 3. Receive a SPLIT_UPDATE with {name:"test", "sets":["x", "y"]}. No changes in storage.
         */
    }

    @Test
    public void sdkWithSetsReceivesSplitKill() {
        /*
         * Initialize a factory with a & b sets configured.
         *
         * 1. Receive a SPLIT_UPDATE with {name:"test", "sets":["a", "b"]}. It should process it since is part of the config.Sets
         *
         * 2. Receive a SPLIT_KILL with {cn:2, name:"test", "defaultTreatment":"off" }. The featureFlag should be removed and a fetch should be performed.
         */
    }

    @Test
    public void sdkWithSetsReceivesSplitKillForNonExistingFeatureFlag() {
        /*
         * Initialize a factory with a & b sets configured.
         *
         * 1. Receive a SPLIT_KILL with {cn:2, name:"test", "defaultTreatment":"off" }. No changes in storage, a fetch should be performed.
         */
    }

    @Nullable
    private SplitClient getReadyClient(
            Context mContext,
            SplitRoomDatabase splitRoomDatabase,
            boolean streamingEnabled,
            String... sets) throws IOException, InterruptedException {
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
                .streamingEnabled(streamingEnabled)
                .eventFlushInterval(1000)
                .build();
CountDownLatch authLatch = new CountDownLatch(1);
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put("splitChanges", (uri, httpMethod, body) -> {
            String since = getSinceFromUri(uri);

            if (since.equals("-1")) {
                return new HttpResponseMock(200, loadSplitChangeWithSet(2));
            } else {
                return new HttpResponseMock(200, IntegrationHelper.emptySplitChanges(1602796638344L, 1602796638344L));
            }
        });
        responses.put("mySegments/CUSTOMER_ID", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));
        responses.put("v2/auth", (uri, httpMethod, body) -> {
            authLatch.countDown();
            return new HttpResponseMock(200, IntegrationHelper.streamingEnabledToken());
        });

        HttpResponseMockDispatcher httpResponseMockDispatcher = IntegrationHelper.buildDispatcher(responses);

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                config,
                mContext,
                new HttpClientMock(httpResponseMockDispatcher),
                splitRoomDatabase, null, null, null);

        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitClient client = splitFactory.client();
        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                readyLatch.countDown();
            }
        });

        boolean await = readyLatch.await(5, TimeUnit.SECONDS);
        boolean authAwait = authLatch.await(5, TimeUnit.SECONDS);

        return (await && authAwait) ? client : null;
    }

    private String loadSplitChangeWithSet(int setsCount) {
        String change = fileHelper.loadFileContent(mContext, "split_changes_flag_set-" + setsCount + ".json");
        SplitChange parsedChange = Json.fromJson(change, SplitChange.class);
        parsedChange.since = parsedChange.till;

        return Json.toJson(parsedChange);
    }
}
