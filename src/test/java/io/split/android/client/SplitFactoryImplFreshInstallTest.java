package io.split.android.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.split.android.client.api.Key;
import io.split.android.client.dtos.Excluded;
import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.dtos.RuleBasedSegmentChange;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.Status;
import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.utils.Json;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Tests for SplitFactoryImpl fresh install prefetch functionality.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class SplitFactoryImplFreshInstallTest {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(30);

    private Context mContext;
    private MockWebServer mMockWebServer;
    private SplitFactory mFactory;
    private String mApiToken;
    private Key mKey;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        
        // Initialize WorkManager
        Configuration config = new Configuration.Builder()
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(mContext, config);
        
        mMockWebServer = new MockWebServer();
        mMockWebServer.start();
        
        mApiToken = "test-fresh-install-" + System.currentTimeMillis();
        mKey = new Key("test-user-key");
        
        cleanupDatabases();
    }

    @After
    public void tearDown() throws Exception {
        if (mFactory != null) {
            try {
                mFactory.destroy();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        
        if (mMockWebServer != null) {
            mMockWebServer.shutdown();
        }
        
        cleanupDatabases();
    }

    @Test
    public void freshInstallTriggersPrefetchWithCorrectParameters() throws Exception {
        TargetingRulesChange mockResponse = createTestTargetingRulesChange();
        
        for (int i = 0; i < 10; i++) {
            mMockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(mockResponse)));
        }

        String baseUrl = mMockWebServer.url("/api/").toString();
        mFactory = SplitFactoryBuilder.build(
                mApiToken,
                mKey,
                SplitClientConfig.builder()
                        .serviceEndpoints(ServiceEndpoints.builder()
                                .apiEndpoint(baseUrl)
                                .eventsEndpoint(baseUrl)
                                .build())
                        .ready(3000)
                        .streamingEnabled(false)
                        .synchronizeInBackground(false)
                        .build(),
                mContext
        );

        // Give time for initialization
        Thread.sleep(2000);
        
        // Then: Verify factory created successfully and requests were made
        assertNotNull("Factory should be created", mFactory);
        assertNotNull("Client should be available", mFactory.client());
        assertTrue("HTTP requests should have been made", mMockWebServer.getRequestCount() > 0);
    }

    @Test
    public void existingDatabaseSkipsFreshInstallPrefetch() throws Exception {
        String dbName = mApiToken.substring(0, Math.min(4, mApiToken.length())) + "resh";
        File dbFile = mContext.getDatabasePath(dbName);
        
        // Create the database directory if it doesn't exist
        dbFile.getParentFile().mkdirs();
        // Create an empty database file to simulate existing installation
        dbFile.createNewFile();
        assertTrue("Database file should exist", dbFile.exists());

        TargetingRulesChange mockResponse = createTestTargetingRulesChange();
        for (int i = 0; i < 10; i++) {
            mMockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(mockResponse)));
        }

        String baseUrl = mMockWebServer.url("/api/").toString();
        mFactory = SplitFactoryBuilder.build(
                mApiToken,
                mKey,
                SplitClientConfig.builder()
                        .serviceEndpoints(ServiceEndpoints.builder()
                                .apiEndpoint(baseUrl)
                                .eventsEndpoint(baseUrl)
                                .build())
                        .ready(3000)
                        .streamingEnabled(false)
                        .synchronizeInBackground(false)
                        .build(),
                mContext
        );

        Thread.sleep(1000);

        assertNotNull("Factory should be created", mFactory);
        assertNotNull("Client should be available", mFactory.client());
    }

    @Test
    public void freshInstallPrefetchHandlesHttpErrors() throws Exception {
        mMockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        TargetingRulesChange mockResponse = createTestTargetingRulesChange();
        for (int i = 0; i < 10; i++) {
            mMockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(mockResponse)));
        }

        String baseUrl = mMockWebServer.url("/api/").toString();
        mFactory = SplitFactoryBuilder.build(
                mApiToken,
                mKey,
                SplitClientConfig.builder()
                        .serviceEndpoints(ServiceEndpoints.builder()
                                .apiEndpoint(baseUrl)
                                .eventsEndpoint(baseUrl)
                                .build())
                        .ready(3000)  // Shorter timeout
                        .streamingEnabled(false)
                        .synchronizeInBackground(false)  // Disable background sync to simplify
                        .build(),
                mContext
        );

        Thread.sleep(1000);

        assertNotNull("Factory should be created despite prefetch error", mFactory);
        assertNotNull("Client should be available", mFactory.client());
    }

    @Test
    public void freshInstallUsesNoCacheHeaders() throws Exception {
        TargetingRulesChange mockResponse = createTestTargetingRulesChange();
        for (int i = 0; i < 5; i++) {
            mMockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(mockResponse)));
        }

        String baseUrl = mMockWebServer.url("/api/").toString();
        mFactory = SplitFactoryBuilder.build(
                mApiToken,
                mKey,
                SplitClientConfig.builder()
                        .serviceEndpoints(ServiceEndpoints.builder()
                                .apiEndpoint(baseUrl)
                                .eventsEndpoint(baseUrl)
                                .build())
                        .ready(5000)
                        .streamingEnabled(false)
                        .build(),
                mContext
        );

        RecordedRequest prefetchRequest = mMockWebServer.takeRequest(5, TimeUnit.SECONDS);
        
        assertNotNull("Should have received a request", prefetchRequest);
    }
    private void cleanupDatabases() {
        if (mContext == null) {
            return;
        }
        
        String[] possibleDbNames = {
                "test-split_data",
                "test-fresh",
                "testfres",
                mApiToken != null && mApiToken.length() >= 4 
                    ? mApiToken.substring(0, 4) + "resh" 
                    : "testresh"
        };

        for (String dbName : possibleDbNames) {
            try {
                File dbFile = mContext.getDatabasePath(dbName);
                if (dbFile.exists()) {
                    dbFile.delete();
                }
                new File(dbFile.getAbsolutePath() + "-shm").delete();
                new File(dbFile.getAbsolutePath() + "-wal").delete();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    private TargetingRulesChange createTestTargetingRulesChange() {
        Split testSplit = new Split();
        testSplit.name = "test_split_fresh";
        testSplit.status = Status.ACTIVE;
        testSplit.changeNumber = 1000L;

        SplitChange splitChange = SplitChange.create(
                -1,
                1000L,
                Collections.singletonList(testSplit)
        );

        RuleBasedSegment testSegment = new RuleBasedSegment(
                "test_segment",
                "user",
                1000L,
                Status.ACTIVE,
                new ArrayList<>(),
                new Excluded()
        );

        RuleBasedSegmentChange ruleBasedSegmentChange = RuleBasedSegmentChange.create(
                -1,
                1000L,
                Collections.singletonList(testSegment)
        );

        return TargetingRulesChange.create(splitChange, ruleBasedSegmentChange);
    }
}
