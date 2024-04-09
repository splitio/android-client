package tests.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static helper.IntegrationHelper.ResponseClosure.getSinceFromUri;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.TestingConfig;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import tests.integration.shared.TestingHelper;

public class FlagsSpecInRequestTest {

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private HttpClientMock mHttpClient;
    private SplitRoomDatabase mDatabase;
    private AtomicReference<String> mQueryString;

    @Before
    public void setUp() throws IOException {
        mDatabase = DatabaseHelper.getTestDatabase(mContext);
        mDatabase.clearAllTables();
        mQueryString = new AtomicReference<>();
        mHttpClient = new HttpClientMock(getDispatcher());
    }

    @Test
    public void queryStringContainsFlagsSpec() throws InterruptedException {
        TestingConfig testingConfig = new TestingConfig();
        testingConfig.setFlagsSpec("1.1");
        initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient, testingConfig);

        assertEquals("s=1.1&since=-1", mQueryString.get());
    }

    @Test
    public void nullFlagsSpecValueOmitsQueryParam() throws InterruptedException {
        TestingConfig testingConfig = new TestingConfig();
        testingConfig.setFlagsSpec(null);
        initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient, testingConfig);

        assertEquals("since=-1", mQueryString.get());
    }

    @Test
    public void newFlagsSpecIsUpdatedInDatabase() throws InterruptedException {
        mDatabase.generalInfoDao().update(new GeneralInfoEntity("flagsSpec", "1.1"));
        TestingConfig testingConfig = new TestingConfig();
        testingConfig.setFlagsSpec("1.2");
        initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient, testingConfig);

        assertEquals("1.2", mDatabase.generalInfoDao().getByName("flagsSpec").getStringValue());
    }

    @Test
    public void newFlagsSpecIsUsedInRequest() throws InterruptedException {
        mDatabase.generalInfoDao().update(new GeneralInfoEntity("flagsSpec", "1.1"));

        TestingConfig testingConfig = new TestingConfig();
        testingConfig.setFlagsSpec("1.2");
        initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient, testingConfig);

        assertEquals("s=1.2&since=-1", mQueryString.get());
    }

    @Test
    public void featureFlagsAreRemovedWhenFlagsSpecChanges() throws InterruptedException {
        mDatabase.generalInfoDao().update(new GeneralInfoEntity("flagsSpec", "1.1"));
        mDatabase.splitDao().insert(newSplitEntity("split_1", "traffic_type", Collections.emptySet()));
        mDatabase.splitDao().insert(newSplitEntity("split_2", "traffic_type", Collections.emptySet()));

        TestingConfig testingConfig = new TestingConfig();
        testingConfig.setFlagsSpec("1.2");
        int initialFlagsSize = mDatabase.splitDao().getAll().size();

        initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient, testingConfig);

        assertEquals("1.2", mDatabase.generalInfoDao().getByName("flagsSpec").getStringValue());
        assertEquals(2, initialFlagsSize);
        assertEquals(30, mDatabase.splitDao().getAll().size());
    }

    private HttpResponseMockDispatcher getDispatcher() {
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put("splitChanges", (uri, httpMethod, body) -> {
            String queryString = uri.getQuery();
            mQueryString.set(queryString);
            String since = getSinceFromUri(uri);
            if (since.equals("-1")) {
                return new HttpResponseMock(200, loadSplitChanges());
            } else {
                return new HttpResponseMock(200, IntegrationHelper.emptySplitChanges(1602796638344L, 1602796638344L));
            }
        });

        responses.put("mySegments/CUSTOMER_ID", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));

        return IntegrationHelper.buildDispatcher(responses);
    }

    private void initSplitFactory(TestableSplitConfigBuilder builder, HttpClientMock httpClient, TestingConfig testingConfig) throws InterruptedException {
        CountDownLatch innerLatch = new CountDownLatch(1);
        SplitFactory factory = IntegrationHelper.buildFactory(
                "sdk_key_1",
                IntegrationHelper.dummyUserKey(),
                builder.build(),
                mContext,
                httpClient,
                mDatabase,
                null,
                testingConfig);

        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(innerLatch));
        boolean await = innerLatch.await(5, TimeUnit.SECONDS);
        if (!await) {
            fail("Client is not ready");
        }
    }

    private String loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        String change = fileHelper.loadFileContent(mContext, "split_changes_1.json");
        SplitChange parsedChange = Json.fromJson(change, SplitChange.class);
        parsedChange.since = parsedChange.till;
        return Json.toJson(parsedChange);
    }

    private static SplitEntity newSplitEntity(String name, String trafficType, Set<String> sets) {
        SplitEntity entity = new SplitEntity();
        String setsString = String.join(",", sets);
        entity.setName(name);
        entity.setBody(String.format(IntegrationHelper.JSON_SPLIT_WITH_TRAFFIC_TYPE_TEMPLATE, name, 9999L, trafficType, setsString));

        return entity;
    }
}
