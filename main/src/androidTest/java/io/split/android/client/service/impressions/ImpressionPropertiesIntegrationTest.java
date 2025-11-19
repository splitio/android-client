package io.split.android.client.service.impressions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static helper.IntegrationHelper.getSinceFromUri;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import fake.LifecycleManagerStub;
import fake.SynchronizerSpyImpl;
import helper.DatabaseHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.EvaluationOptions;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.synchronizer.SynchronizerSpy;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import tests.integration.shared.TestingHelper;

public class ImpressionPropertiesIntegrationTest {

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private AtomicInteger mImpressionsLoggedCount;
    private AtomicBoolean mPropertiesReceived;
    private HttpClientMock mHttpClient;
    private SplitRoomDatabase mDatabase;
    private LifecycleManagerStub mLifecycleManager;
    private SynchronizerSpy mSynchronizerSpy;

    @Before
    public void setUp() throws IOException {
        mImpressionsLoggedCount = new AtomicInteger(0);
        mPropertiesReceived = new AtomicBoolean(false);
        mDatabase = DatabaseHelper.getTestDatabase(mContext);
        mDatabase.clearAllTables();
        mHttpClient = new HttpClientMock(getDispatcher());
        mLifecycleManager = new LifecycleManagerStub();
        mSynchronizerSpy = new SynchronizerSpyImpl();
        mLifecycleManager.register(mSynchronizerSpy);
    }

    /**
     * Tests that impressions include properties when provided during evaluation.
     * Verifies that properties are correctly passed to the impression listener
     * and stored in the database.
     */
    @Test
    public void impressionsIncludePropertiesWhenProvided() throws InterruptedException {
        // Initialize Split SDK with impression listener
        CountDownLatch countDownLatch = new CountDownLatch(1);
        SplitClient client = initSplitFactory(new TestableSplitConfigBuilder()
                .impressionsMode(ImpressionsMode.OPTIMIZED)
                .enableDebug()
                .impressionListener(new ImpressionListener() {
                    @Override
                    public void log(Impression impression) {
                        mImpressionsLoggedCount.incrementAndGet();
                        if (impression.properties() != null && !impression.properties().isEmpty()) {
                            mPropertiesReceived.set(true);
                        }
                        countDownLatch.countDown();
                    }

                    @Override
                    public void close() {
                        // No-op
                    }
                }), mHttpClient).client();

        // Create properties map
        Map<String, Object> properties = new HashMap<>();
        properties.put("string_prop", "value");
        properties.put("number_prop", 42);
        properties.put("bool_prop", true);

        // Get treatment with properties
        evaluateWithProperties(client, properties);

        boolean await = countDownLatch.await(5, TimeUnit.SECONDS);

        // Verify impressions were recorded with properties
        Thread.sleep(200);
        List<ImpressionEntity> impressionEntities = mDatabase.impressionDao().getAll();
        assertEquals(1, impressionEntities.size());
        assertEquals(1, mImpressionsLoggedCount.get());
        assertTrue(await);
        assertTrue(mPropertiesReceived.get());
    }

    /**
     * Tests that impressions without properties do not include a properties field.
     * Verifies that when evaluations are done without properties, the impression
     * listener and database do not receive properties.
     */
    @Test
    public void impressionsWithoutPropertiesDoNotIncludePropertiesField() throws InterruptedException {
        // Initialize Split SDK with impression listener
        CountDownLatch countDownLatch = new CountDownLatch(1);
        SplitClient client = initSplitFactory(new TestableSplitConfigBuilder()
                .impressionsMode(ImpressionsMode.DEBUG)
                .enableDebug()
                .impressionListener(new ImpressionListener() {
                    @Override
                    public void log(Impression impression) {
                        mImpressionsLoggedCount.incrementAndGet();
                        if (impression.properties() != null && !impression.properties().isEmpty()) {
                            mPropertiesReceived.set(true);
                        }
                        countDownLatch.countDown();
                    }

                    @Override
                    public void close() {
                        // No-op
                    }
                }), mHttpClient).client();

        // Get treatment without properties
        client.getTreatment("FACUNDO_TEST");

        // Wait for impression processing
        boolean await = countDownLatch.await(5, TimeUnit.SECONDS);

        // Verify impressions were recorded without properties
        Thread.sleep(200);
        List<ImpressionEntity> impressionEntities = mDatabase.impressionDao().getAll();
        assertTrue(await);
        assertEquals(1, impressionEntities.size());
        assertEquals(1, mImpressionsLoggedCount.get());
        assertFalse(mPropertiesReceived.get());
    }

    /**
     * Tests that impressions with different properties are not deduplicated in OPTIMIZED mode.
     * Verifies that when the same feature flag is evaluated multiple times with different
     * properties, each impression is tracked separately.
     */
    @Test
    public void impressionsWithPropertiesAreNotDeduped() throws InterruptedException, IOException {
        // Setup HTTP client to capture impressions requests
        final AtomicReference<String> capturedImpressionPayload = new AtomicReference<>();
        CountDownLatch impressionsLatch = new CountDownLatch(1);
        
        // Create HTTP client with impression capture
        HttpClientMock httpClient = new HttpClientMock(createDispatcher(
            (uri, httpMethod, body) -> {
                capturedImpressionPayload.set(body);
                impressionsLatch.countDown();
                return new HttpResponseMock(200, "{}");
            }
        ));
        
        // Initialize Split SDK with OPTIMIZED mode
        SplitClient client = initSplitFactory(getOptimizedConfigBuilder(), httpClient).client();
        
        // Evaluate the same flag multiple times with different properties
        Map<String, Object> properties1 = createTestProperties("test_value1", 42, true);
        Map<String, Object> properties2 = createTestProperties("test_value2", 43, false);
        
        evaluateWithProperties(client, properties1);
        evaluateWithProperties(client, properties2);
        evaluateWithProperties(client, properties1); // Repeat with same properties
        
        Thread.sleep(500);
        client.flush();
        
        // Wait for impressions to be sent
        boolean await = impressionsLatch.await(5, TimeUnit.SECONDS);
        assertTrue(await);
        
        // Verify the payload
        String payload = capturedImpressionPayload.get();
        assertNotNull("Impressions payload should not be null", payload);
        
        // Count impressions with each property set
        countAndVerifyImpressions(payload, 2, 1);
    }

    /**
     * Tests that impression properties are correctly included in the network payload.
     * Verifies that properties provided during evaluation are serialized and sent
     * to the backend in the correct format.
     */
    @Test
    public void impressionsPayloadIncludesProperties() throws InterruptedException, IOException {
        // Setup HTTP client to capture impressions requests
        final AtomicReference<String> capturedImpressionPayload = new AtomicReference<>();
        CountDownLatch impressionsLatch = new CountDownLatch(1);
        
        // Create HTTP client with impression capture
        HttpClientMock httpClient = new HttpClientMock(createDispatcher(
            (uri, httpMethod, body) -> {
                capturedImpressionPayload.set(body);
                impressionsLatch.countDown();
                return new HttpResponseMock(200, "{}");
            }
        ));
        
        // Initialize Split SDK with DEBUG mode
        SplitClient client = initSplitFactory(getDebugConfigBuilder(), httpClient).client();
        
        // Evaluate flags with and without properties
        evaluateWithoutProperties(client);
        evaluateWithDifferentProperties(client);

        Thread.sleep(500);
        client.flush();
        
        // Wait for impressions to be sent
        boolean await = impressionsLatch.await(5, TimeUnit.SECONDS);
        assertTrue(await);
        
        // Verify the payload
        String payload = capturedImpressionPayload.get();
        assertNotNull("Impressions payload should not be null", payload);
        
        // Deserialize and verify impressions
        verifyImpressionPayload(payload);
    }

    /**
     * Tests that impressions in NONE mode still track properties in the impression listener.
     * Verifies that even when impressions are not sent to the backend (NONE mode),
     * properties are still passed to the impression listener.
     */
    @Test
    public void impressionsInNoneModeStillTrackPropertiesInListener() throws InterruptedException {
        // Reset counters for the test
        mImpressionsLoggedCount.set(0);
        mPropertiesReceived.set(false);
        mDatabase.clearAllTables();
        
        // Create a latch to wait for impression listener
        CountDownLatch listenerLatch = new CountDownLatch(1);
        AtomicReference<String> capturedProperties = new AtomicReference<>();
        
        // Initialize Split SDK with NONE mode and impression listener
        SplitClient client = initSplitFactory(new TestableSplitConfigBuilder()
                .impressionsMode(ImpressionsMode.NONE) // Use NONE mode which doesn't send impressions to backend
                .enableDebug()
                .impressionListener(new ImpressionListener() {
                    @Override
                    public void log(Impression impression) {
                        mImpressionsLoggedCount.incrementAndGet();
                        if (impression.properties() != null && !impression.properties().isEmpty()) {
                            capturedProperties.set(impression.properties());
                            mPropertiesReceived.set(true);
                            listenerLatch.countDown();
                        }
                    }

                    @Override
                    public void close() {
                        // No-op
                    }
                }), mHttpClient).client();

        // Create test properties
        Map<String, Object> properties = createTestProperties("test_value", 42, true);
        
        // Evaluate with properties
        evaluateWithProperties(client, properties);
        
        // Wait for impression listener to be called
        boolean await = listenerLatch.await(5, TimeUnit.SECONDS);
        
        // Verify impressions were tracked in listener but not in storage
        assertTrue("Impression listener should be called", await);
        assertEquals("Should have 1 impression logged", 1, mImpressionsLoggedCount.get());
        assertTrue("Properties should be received in listener", mPropertiesReceived.get());
        assertNotNull("Properties should be captured", capturedProperties.get());
        assertTrue("Properties should contain test value", capturedProperties.get().contains("test_value"));
        
        // Verify no impressions were stored (NONE mode)
        Thread.sleep(200); // Wait for any potential DB operations
        List<ImpressionEntity> impressionEntities = mDatabase.impressionDao().getAll();
        assertEquals("No impressions should be stored in NONE mode", 0, impressionEntities.size());
    }
    
    /**
     * Tests that impressions in DEBUG mode track all properties without deduplication.
     * Verifies that in DEBUG mode, all impressions with properties are tracked and
     * sent to the backend, regardless of frequency or similarity.
     */
    @Test
    public void impressionsInDebugModeTrackAllProperties() throws InterruptedException, IOException {
        // Setup HTTP client to capture impressions requests
        final AtomicReference<String> capturedImpressionPayload = new AtomicReference<>();
        CountDownLatch impressionsLatch = new CountDownLatch(1);
        
        // Create HTTP client with impression capture
        HttpClientMock httpClient = new HttpClientMock(createDispatcher(
            (uri, httpMethod, body) -> {
                capturedImpressionPayload.set(body);
                impressionsLatch.countDown();
                return new HttpResponseMock(200, "{}");
            }
        ));
        
        // Initialize Split SDK with DEBUG mode
        SplitClient client = initSplitFactory(getDebugConfigBuilder(), httpClient).client();
        
        // Create different property sets
        Map<String, Object> properties1 = createTestProperties("test_value1", 42, true);
        Map<String, Object> properties2 = createTestProperties("test_value2", 43, false);
        Map<String, Object> properties3 = createTestProperties("test_value3", 44, true);
        
        // Evaluate with multiple property sets in quick succession
        evaluateWithProperties(client, properties1);
        evaluateWithProperties(client, properties2);
        evaluateWithProperties(client, properties3);
        
        // Add a small delay before flushing to ensure impressions are queued
        Thread.sleep(500);

        // Explicitly flush to ensure impressions are sent
        client.flush();
        
        // Wait for impressions to be sent with increased timeout
        boolean await = impressionsLatch.await(10, TimeUnit.SECONDS);
        assertTrue("Impressions should be sent after flush", await);
        
        // Verify the payload
        String payload = capturedImpressionPayload.get();
        assertNotNull("Impressions payload should not be null", payload);
        
        // Verify all impressions were sent
        Type testImpressionsListType = new TypeToken<List<TestImpressions>>(){}.getType();
        List<TestImpressions> testImpressions = Json.fromJson(payload, testImpressionsListType);
        
        // Count total impressions with properties
        int totalImpressionsWithProperties = 0;
        for (TestImpressions testImpression : testImpressions) {
            for (KeyImpression keyImpression : testImpression.keyImpressions) {
                if (keyImpression.properties != null && !keyImpression.properties.isEmpty()) {
                    totalImpressionsWithProperties++;
                }
            }
        }
        
        // In DEBUG mode, all impressions should be tracked (not deduplicated)
        assertEquals("All impressions with properties should be tracked in DEBUG mode", 
                3, totalImpressionsWithProperties);
        
        // Verify each property set is present
        boolean foundProperties1 = false;
        boolean foundProperties2 = false;
        boolean foundProperties3 = false;
        
        for (TestImpressions testImpression : testImpressions) {
            for (KeyImpression keyImpression : testImpression.keyImpressions) {
                if (keyImpression.properties != null) {
                    if (keyImpression.properties.contains("test_value1")) {
                        foundProperties1 = true;
                    } else if (keyImpression.properties.contains("test_value2")) {
                        foundProperties2 = true;
                    } else if (keyImpression.properties.contains("test_value3")) {
                        foundProperties3 = true;
                    }
                }
            }
        }
        
        assertTrue("Should find impression with first property set", foundProperties1);
        assertTrue("Should find impression with second property set", foundProperties2);
        assertTrue("Should find impression with third property set", foundProperties3);
    }

    private HttpResponseMockDispatcher getDispatcher() {
        return createDispatcher(null);
    }

    private HttpResponseMockDispatcher createDispatcher(IntegrationHelper.ResponseClosure impressionsHandler) {
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        
        // Add standard responses
        responses.put(IntegrationHelper.ServicePath.SPLIT_CHANGES, (uri, httpMethod, body) -> {
            String since = getSinceFromUri(uri);
            if (since.equals("-1")) {
                return new HttpResponseMock(200, loadSplitChanges());
            } else {
                return new HttpResponseMock(200, IntegrationHelper.emptySplitChanges(1602796638344L, 1602796638344L));
            }
        });

        responses.put(IntegrationHelper.ServicePath.MEMBERSHIPS + "/" + "/CUSTOMER_ID", (uri, httpMethod, body) ->
                new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));
        
        // Add custom impressions handler if provided
        if (impressionsHandler != null) {
            responses.put(IntegrationHelper.ServicePath.IMPRESSIONS, impressionsHandler);
        }
                
        return IntegrationHelper.buildDispatcher(responses);
    }

    private SplitFactory initSplitFactory(TestableSplitConfigBuilder builder, HttpClientMock httpClient) throws InterruptedException {
        CountDownLatch innerLatch = new CountDownLatch(1);
        SplitFactory factory = IntegrationHelper.buildFactory(
                "sdk_key_1",
                new Key("CUSTOMER_ID"),
                builder.build(),
                mContext,
                httpClient,
                mDatabase,
                mSynchronizerSpy,
                null,
                mLifecycleManager);

        SplitClient client = factory.client();
        client.on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(innerLatch));
        boolean await = innerLatch.await(5, TimeUnit.SECONDS);
        if (!await) {
            fail("Client is not ready");
        }
        return factory;
    }

    private String loadSplitChanges() {
        return IntegrationHelper.loadSplitChanges(mContext, "split_changes_1.json");
    }

    private static void evaluateWithProperties(SplitClient client, Map<String, Object> properties) {
        client.getTreatment("FACUNDO_TEST", null, new EvaluationOptions(properties));
    }

    private Map<String, Object> createTestProperties(String stringValue, int numberValue, boolean boolValue) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("string_prop", stringValue);
        properties.put("number_prop", numberValue);
        properties.put("bool_prop", boolValue);
        return properties;
    }
    
    private void evaluateWithoutProperties(SplitClient client) {
        client.getTreatment("FACUNDO_TEST");
    }
    
    private void evaluateWithDifferentProperties(SplitClient client) {
        Map<String, Object> properties1 = createTestProperties("test_value1", 42, true);
        Map<String, Object> properties2 = createTestProperties("test_value2", 43, false);
        
        evaluateWithProperties(client, properties1);
        evaluateWithProperties(client, properties2);
    }
    
    private void verifyImpressionPayload(String payload) {
        // Deserialize the payload to verify properties
        Type testImpressionsListType = new TypeToken<List<TestImpressions>>(){}.getType();
        List<TestImpressions> testImpressions = Json.fromJson(payload, testImpressionsListType);
        
        // Verify we have impressions
        assertNotNull("Deserialized impressions should not be null", testImpressions);
        assertFalse("Impressions list should not be empty", testImpressions.isEmpty());
        
        // Check for impressions with and without properties
        boolean foundWithoutProperties = false;
        boolean foundWithProperties1 = false;
        boolean foundWithProperties2 = false;
        
        for (TestImpressions testImpression : testImpressions) {
            for (KeyImpression keyImpression : testImpression.keyImpressions) {
                if (keyImpression.properties == null) {
                    foundWithoutProperties = true;
                } else if (keyImpression.properties.contains("test_value1") && 
                           keyImpression.properties.contains("42") && 
                           keyImpression.properties.contains("true")) {
                    foundWithProperties1 = true;
                } else if (keyImpression.properties.contains("test_value2") && 
                           keyImpression.properties.contains("43") && 
                           keyImpression.properties.contains("false")) {
                    foundWithProperties2 = true;
                }
            }
        }
        
        assertTrue("Should find impression without properties", foundWithoutProperties);
        assertTrue("Should find impression with first set of properties", foundWithProperties1);
        assertTrue("Should find impression with second set of properties", foundWithProperties2);
    }

    /**
     * Counts and verifies impressions with different property sets
     * @param payload The JSON payload to analyze
     * @param expectedCount1 Expected count of impressions with first property set
     * @param expectedCount2 Expected count of impressions with second property set
     */
    private void countAndVerifyImpressions(String payload, int expectedCount1, int expectedCount2) {
        // Deserialize the payload to verify properties
        Type testImpressionsListType = new TypeToken<List<TestImpressions>>(){}.getType();
        List<TestImpressions> testImpressions = Json.fromJson(payload, testImpressionsListType);
        
        // Verify we have impressions
        assertNotNull("Deserialized impressions should not be null", testImpressions);
        assertFalse("Impressions list should not be empty", testImpressions.isEmpty());
        
        // Count impressions with each property set
        int impressionsWithProperties1 = 0;
        int impressionsWithProperties2 = 0;
        
        for (TestImpressions testImpression : testImpressions) {
            for (KeyImpression keyImpression : testImpression.keyImpressions) {
                if (keyImpression.properties != null) {
                    if (keyImpression.properties.contains("test_value1") && 
                        keyImpression.properties.contains("42") && 
                        keyImpression.properties.contains("true")) {
                        impressionsWithProperties1++;
                    } else if (keyImpression.properties.contains("test_value2") && 
                               keyImpression.properties.contains("43") && 
                               keyImpression.properties.contains("false")) {
                        impressionsWithProperties2++;
                    }
                }
            }
        }
        
        assertEquals("Unexpected count of impressions with first property set", 
                expectedCount1, impressionsWithProperties1);
        assertEquals("Unexpected count of impressions with second property set", 
                expectedCount2, impressionsWithProperties2);
    }

    private TestableSplitConfigBuilder getDebugConfigBuilder() {
        return new TestableSplitConfigBuilder()
                .impressionsMode(ImpressionsMode.DEBUG)
                .enableDebug();
    }
    
    private TestableSplitConfigBuilder getOptimizedConfigBuilder() {
        return new TestableSplitConfigBuilder()
                .impressionsMode(ImpressionsMode.OPTIMIZED)
                .enableDebug();
    }
}
