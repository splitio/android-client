package tests.integration.matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static helper.IntegrationHelper.getSinceFromUri;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.DatabaseHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitManager;
import io.split.android.client.api.Key;
import io.split.android.client.api.SplitView;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.SplitRoomDatabase;
import tests.integration.shared.TestingHelper;

public class PrerequisitesTest {

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private HttpClientMock mHttpClient;
    private SplitRoomDatabase mDatabase;
    private CountDownLatch mAuthLatch;

    @Before
    public void setUp() throws IOException {
        mAuthLatch = new CountDownLatch(1);
        mDatabase = DatabaseHelper.getTestDatabase(mContext);
        mDatabase.clearAllTables();
        mHttpClient = new HttpClientMock(getDispatcher());
    }

    @Test
    public void prerequisiteNotSatisfiedForExcludedKey() throws InterruptedException {
        // Test that a key excluded from the rule-based segment doesn't satisfy the prerequisite
        SplitFactory splitFactory = initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient, "test");
        SplitClient client = splitFactory.client();

        String treatment = client.getTreatment("always_on_if_prerequisite");

        assertEquals("off", treatment);
    }

    @Test
    public void prerequisiteSatisfiedForSplitIoUser() throws InterruptedException {
        // Test that a key with @split.io satisfies the rule-based segment condition and the prerequisite
        SplitFactory splitFactory = initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient, "bilal@split.io");
        SplitClient client = splitFactory.client();

        String treatment = client.getTreatment("always_on_if_prerequisite");

        assertEquals("on", treatment);
    }

    @Test
    public void prerequisiteNotSatisfiedForNonSplitIoUser() throws InterruptedException {
        // Test that a key without @split.io doesn't satisfy the rule-based segment condition
        SplitFactory splitFactory = initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient, "test");
        SplitClient client = splitFactory.client();

        String treatment = client.getTreatment("always_on_if_prerequisite");

        assertEquals("off", treatment);
    }

    @Test
    public void splitManagerContainsPrerequisiteInfo() throws InterruptedException {
        // Test that the SplitManager returns the correct prerequisite information
        SplitFactory splitFactory = initSplitFactory(new TestableSplitConfigBuilder(), mHttpClient, "test");
        SplitManager manager = splitFactory.manager();

        SplitView split = manager.split("always_on_if_prerequisite");

        assertNotNull(split);
        assertEquals("user", split.trafficType);
        assertEquals("always_on_if_prerequisite", split.name);
        assertFalse(split.killed);
        assertEquals(5, split.changeNumber);
        assertEquals("off", split.defaultTreatment);
        assertEquals(1, split.prerequisites.size());
        assertEquals("rbs_test_flag", split.prerequisites.get(0).getFlagName());
        assertEquals(1, split.prerequisites.get(0).getTreatments().size());
        assertTrue(split.prerequisites.get(0).getTreatments().contains("v1"));
    }

    private HttpResponseMockDispatcher getDispatcher() {
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put(IntegrationHelper.ServicePath.SPLIT_CHANGES, (uri, httpMethod, body) -> {
            String since = getSinceFromUri(uri);
            if (since.equals("-1")) {
                return new HttpResponseMock(200, loadSplitChanges());
            } else {
                return new HttpResponseMock(200, IntegrationHelper.emptySplitChanges(1602796638344L, 1602796638344L));
            }
        });

        responses.put(IntegrationHelper.ServicePath.MEMBERSHIPS + "/" + "/test", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));
        responses.put(IntegrationHelper.ServicePath.MEMBERSHIPS + "/" + "/bilal@split.io", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));

        responses.put(IntegrationHelper.ServicePath.AUTH, (uri, httpMethod, body) -> {
            mAuthLatch.countDown();
            return new HttpResponseMock(200, IntegrationHelper.streamingEnabledToken());
        });

        return IntegrationHelper.buildDispatcher(responses);
    }

    private SplitFactory initSplitFactory(TestableSplitConfigBuilder builder, HttpClientMock httpClient, String matchingKey) throws InterruptedException {
        CountDownLatch innerLatch = new CountDownLatch(1);
        SplitFactory factory = IntegrationHelper.buildFactory(
                "sdk_key_1",
                new Key(matchingKey),
                builder
                        .enableDebug()
                        .build(),
                mContext,
                httpClient,
                mDatabase,
                null);

        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(innerLatch));
        boolean await = innerLatch.await(5, TimeUnit.SECONDS);
        if (!await) {
            fail("Client is not ready");
        }

        return factory;
    }

    private String loadSplitChanges() {
        return IntegrationHelper.loadSplitChanges(mContext, "splitchanges_prerequisites.json");
    }
}

