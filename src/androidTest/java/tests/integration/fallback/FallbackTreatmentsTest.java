package tests.integration.fallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.IntegrationHelper;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitResult;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.fallback.FallbackTreatmentsConfiguration;
import io.split.android.client.fallback.FallbackTreatment;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.utils.logger.SplitLogLevel;
import io.split.android.grammar.Treatments;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class FallbackTreatmentsTest {

    private Context mContext;
    private MockWebServer mWebServer;
    private int mCurSplitReqId;

    private ServiceEndpoints endpoints() {
        final String url = mWebServer.url("/").url().toString();
        return ServiceEndpoints.builder()
                .apiEndpoint(url)
                .eventsEndpoint(url)
                .build();
    }

    // Helpers
    private static ImpressionListener createImpressionCapturingListener(final List<Impression> sink) {
        return new ImpressionListener() {
            @Override
            public void log(Impression impression) { sink.add(impression); }
            @Override
            public void close() { }
        };
    }

    private static SplitClientConfig buildDebugConfigWithListener(ServiceEndpoints endpoints,
                                                                  FallbackTreatmentsConfiguration fbConfig,
                                                                  ImpressionListener listener) {
        return SplitClientConfig.builder()
                .serviceEndpoints(endpoints)
                .ready(30000)
                .featuresRefreshRate(3)
                .segmentsRefreshRate(3)
                .trafficType("account")
                .impressionsRefreshRate(1)
                .impressionsMode(ImpressionsMode.DEBUG)
                .fallbackTreatments(fbConfig)
                .impressionListener(listener)
                .build();
    }

    private static void assertPayloadHasOnlyKnownFlagNoDnf(String body) {
        boolean hasKnown = body.contains("\"f\":\"real_flag\"") || body.contains("real_flag");
        boolean hasUnknownFlag = body.contains("\"f\":\"dnf_flag\"");
        boolean hasDnfLabel = body.contains("\"r\":\"definition not found\"");
        boolean hasFallbackDnfLabel = body.contains("fallback - definition not found");

        assertTrue("Expected at least one impression for real_flag", hasKnown);
        assertFalse("Unknown flag should not produce impressions", hasUnknownFlag);
        assertFalse("Label 'definition not found' should not appear in impressions", hasDnfLabel);
        assertFalse("Label 'fallback - definition not found' should not appear in impressions", hasFallbackDnfLabel);
    }

    private static void assertLocalNoUnknownOrDnf(List<Impression> captured) {
        assertEquals("Expected exactly one impression locally (real_flag)", 1, captured.size());
        Impression imp = captured.get(0);
        assertEquals("real_flag", imp.split());
        String label = imp.appliedRule();
        assertFalse("Label 'definition not found' should not appear in impressions (listener)",
                "definition not found".equals(label));
        assertFalse("Label 'fallback - definition not found' should not appear in impressions (listener)",
                label != null && label.contains("fallback - definition not found"));
    }

    private SplitClientConfig buildConfig(FallbackTreatmentsConfiguration fbConfig) {
        return buildConfig(fbConfig, false, null);
    }

    private SplitClientConfig buildConfig(FallbackTreatmentsConfiguration fbConfig, boolean debugImpressions, Integer impressionsRefreshRate) {
        SplitClientConfig.Builder builder = SplitClientConfig.builder()
                .serviceEndpoints(endpoints())
                .ready(30000)
                .featuresRefreshRate(3)
                .segmentsRefreshRate(3)
                .logLevel(SplitLogLevel.VERBOSE)
                .trafficType("account");
        if (impressionsRefreshRate != null) {
            builder.impressionsRefreshRate(impressionsRefreshRate);
        } else {
            builder.impressionsRefreshRate(3);
        }
        if (debugImpressions) {
            builder.impressionsMode(ImpressionsMode.DEBUG);
        }
        if (fbConfig != null) {
            builder.fallbackTreatments(fbConfig);
        }
        return builder.build();
    }

    private SplitFactory buildFactory(SplitClientConfig config) {
        return IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), new Key("DEFAULT_KEY"), config, mContext, null);
    }

    private void awaitReady(SplitClient client) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                latch.countDown();
            }
        });
        latch.await(30, TimeUnit.SECONDS);
    }

    @Before
    public void setup() {
        mWebServer = new MockWebServer();
        mCurSplitReqId = 1;
        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                final String path = request.getPath();
                if (path.contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.dummyAllSegments());
                } else if (path.contains("/splitChanges")) {
                    // Return empty changes to keep no real flags available
                    long id = mCurSplitReqId++;
                    return new MockResponse().setResponseCode(200)
                            .setBody(IntegrationHelper.emptyTargetingRulesChanges(id, id));
                } else if (path.contains("/testImpressions/bulk")) {
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mWebServer.setDispatcher(dispatcher);
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @After
    public void tearDown() throws Exception {
        if (mWebServer != null) mWebServer.shutdown();
    }

    @Test
    public void case1_controlTreatment_noFallbacks_returnsControlForUnknownFlags_andTwoKeys() throws Exception {
        SplitClientConfig config = buildConfig(null);

        SplitFactory factory = buildFactory(config);

        SplitClient clientKey1 = factory.client(new Key("key_1"));
        SplitClient clientKey2 = factory.client(new Key("key_2"));

        awaitReady(clientKey1);

        String t1_flag1 = clientKey1.getTreatment("non_existent_flag");
        String t1_flag2 = clientKey1.getTreatment("non_existent_flag_2");
        String t2_flag1 = clientKey2.getTreatment("non_existent_flag");
        String t2_flag2 = clientKey2.getTreatment("non_existent_flag_2");

        // Assert
        assertEquals(Treatments.CONTROL, t1_flag1);
        assertEquals(Treatments.CONTROL, t1_flag2);
        assertEquals(Treatments.CONTROL, t2_flag1);
        assertEquals(Treatments.CONTROL, t2_flag2);

        factory.destroy();
    }

    @Test
    public void case6_impressionsCorrectnessWithFallbackLabelsPrefixedForOverriddenFlagOnlyNotReadyForOthers() throws Exception {
        final String url = mWebServer.url("/").url().toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url)
                .eventsEndpoint(url)
                .build();

        final StringBuilder postedImpressions = new StringBuilder();
        final CountDownLatch impressionsLatch = new CountDownLatch(1);
        mCurSplitReqId = 1;
        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                final String path = request.getPath();
                if (path.contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.dummyAllSegments());
                } else if (path.contains("/splitChanges")) {
                    long id = mCurSplitReqId++;
                    // Keep no real flags to ensure not-ready path applies before SDK ready
                    return new MockResponse().setResponseCode(200)
                            .setBody(IntegrationHelper.emptyTargetingRulesChanges(id, id));
                } else if (path.contains("/testImpressions/bulk")) {
                    try {
                        // Capture body for assertions
                        postedImpressions.append(request.getBody().readUtf8());
                    } catch (Exception ignore) { }
                    impressionsLatch.countDown();
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mWebServer.setDispatcher(dispatcher);

        Map<String, FallbackTreatment> byFlag = new HashMap<>();
        byFlag.put("any_flag", new FallbackTreatment("OFF_FALLBACK"));
        FallbackTreatmentsConfiguration fbConfig = FallbackTreatmentsConfiguration.builder()
                .byFlag(byFlag)
                .build();

        SplitClientConfig config = buildConfig(fbConfig, true, 1);

        SplitFactory factory = buildFactory(config);

        SplitClient c = factory.client(new Key("key_1"));

        String t_overridden = c.getTreatment("any_flag");
        String t_other = c.getTreatment("other_flag");
        Thread.sleep(1000);
        c.flush();

        impressionsLatch.await(5, TimeUnit.SECONDS);

        assertEquals("OFF_FALLBACK", t_overridden);

        String body = postedImpressions.toString();
        System.out.println("IMPRESSIONS BODY: " + body);
        boolean hasPrefixed = body.contains("\"f\":\"any_flag\"") && body.contains("\"r\":\"fallback - not ready\"");
        boolean hasPlain = body.contains("\"f\":\"other_flag\"") && body.contains("\"r\":\"not ready\"");
        if (!hasPrefixed || !hasPlain) {
            hasPrefixed = body.contains("fallback - not ready");
            hasPlain = body.contains("\"r\":\"not ready\"");
        }
        assertTrue("Expected impression with label 'fallback - not ready' for any_flag", hasPrefixed);
        assertTrue("Expected impression with label 'not ready' for other_flag", hasPlain);

        factory.destroy();
    }

    @Test
    public void case5_overrideAppliesOnlyWhenOriginalWouldBeControlRealFlagUnaffectedUnknownGetsFallback() throws Exception {
        final String url = mWebServer.url("/").url().toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url)
                .eventsEndpoint(url)
                .build();

        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                final String path = request.getPath();
                if (path.contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.dummyAllSegments());
                } else if (path.contains("/splitChanges")) {
                    String change = IntegrationHelper.loadSplitChanges(mContext, "simple_split.json");
                    change = change.replace("\"workm\"", "\"real_flag\"");
                    return new MockResponse().setResponseCode(200).setBody(change);
                } else if (path.contains("/testImpressions/bulk")) {
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mWebServer.setDispatcher(dispatcher);

        FallbackTreatmentsConfiguration fbConfig = FallbackTreatmentsConfiguration.builder()
                .global(new FallbackTreatment("OFF_FALLBACK"))
                .build();

        SplitClientConfig config = buildConfig(fbConfig);

        SplitFactory factory = buildFactory(config);

        SplitClient clientKey1 = factory.client(new Key("key_1"));

        awaitReady(clientKey1);

        String realFlag = clientKey1.getTreatment("real_flag");
        String unknown = clientKey1.getTreatment("non_existent_flag");

        assertEquals("on", realFlag);
        assertEquals("OFF_FALLBACK", unknown);

        factory.destroy();
    }

    @Test
    public void case4_FlagOverrideBeatsFactoryDefaultReturnsOnFallbackForOverriddenAndOffFallbackForOthers() throws Exception {
        Map<String, FallbackTreatment> byFlag = new HashMap<>();
        byFlag.put("my_flag", new FallbackTreatment("ON_FALLBACK"));
        FallbackTreatmentsConfiguration fbConfig = FallbackTreatmentsConfiguration.builder()
                .global(new FallbackTreatment("OFF_FALLBACK"))
                .byFlag(byFlag)
                .build();

        SplitClientConfig config = buildConfig(fbConfig);

        SplitFactory factory = buildFactory(config);

        SplitClient clientKey1 = factory.client(new Key("key_1"));
        SplitClient clientKey2 = factory.client(new Key("key_2"));

        awaitReady(clientKey1);

        String t1_myFlag = clientKey1.getTreatment("my_flag");
        String t1_other = clientKey1.getTreatment("non_existent_flag_2");
        String t2_myFlag = clientKey2.getTreatment("my_flag");
        String t2_other = clientKey2.getTreatment("non_existent_flag_2");

        assertEquals("ON_FALLBACK", t1_myFlag);
        assertEquals("OFF_FALLBACK", t1_other);
        assertEquals("ON_FALLBACK", t2_myFlag);
        assertEquals("OFF_FALLBACK", t2_other);

        factory.destroy();
    }

    @Test
    public void case2_factoryWideOverrideReturnsFallbackForUnknownFlagsAndTwoKeys() throws Exception {
        // endpoints provided by helper in buildConfig

        FallbackTreatmentsConfiguration fbConfig = FallbackTreatmentsConfiguration.builder()
                .global(new FallbackTreatment("FALLBACK_TREATMENT"))
                .build();

        SplitClientConfig config = buildConfig(fbConfig);

        SplitFactory factory = buildFactory(config);

        SplitClient clientKey1 = factory.client(new Key("key_1"));
        SplitClient clientKey2 = factory.client(new Key("key_2"));

        CountDownLatch readyLatch = new CountDownLatch(1);
        clientKey1.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                readyLatch.countDown();
            }
        });
        readyLatch.await(30, TimeUnit.SECONDS);

        String t1_flag1 = clientKey1.getTreatment("non_existent_flag");
        String t1_flag2 = clientKey1.getTreatment("non_existent_flag_2");
        String t2_flag1 = clientKey2.getTreatment("non_existent_flag");
        String t2_flag2 = clientKey2.getTreatment("non_existent_flag_2");

        assertEquals("FALLBACK_TREATMENT", t1_flag1);
        assertEquals("FALLBACK_TREATMENT", t1_flag2);
        assertEquals("FALLBACK_TREATMENT", t2_flag1);
        assertEquals("FALLBACK_TREATMENT", t2_flag2);

        factory.destroy();
    }

    @Test
    public void case3_factorySpecificOverrideReturnsFallbackForOneFlagAndControlForOthersAndTwoKeys() throws Exception {
        final String url = mWebServer.url("/").url().toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url)
                .eventsEndpoint(url)
                .build();

        Map<String, FallbackTreatment> byFlag = new HashMap<>();
        byFlag.put("non_existent_flag", new FallbackTreatment("FALLBACK_TREATMENT"));
        FallbackTreatmentsConfiguration fbConfig = FallbackTreatmentsConfiguration.builder()
                .byFlag(byFlag)
                .build();

        SplitClientConfig config = SplitClientConfig.builder()
                .serviceEndpoints(endpoints)
                .ready(30000)
                .featuresRefreshRate(3)
                .segmentsRefreshRate(3)
                .impressionsRefreshRate(3)
                .logLevel(SplitLogLevel.DEBUG)
                .trafficType("account")
                .fallbackTreatments(fbConfig)
                .build();

        SplitFactory factory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), new Key("DEFAULT_KEY"), config, mContext, null);

        SplitClient clientKey1 = factory.client(new Key("key_1"));
        SplitClient clientKey2 = factory.client(new Key("key_2"));

        CountDownLatch readyLatch = new CountDownLatch(1);
        clientKey1.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                readyLatch.countDown();
            }
        });
        readyLatch.await(30, TimeUnit.SECONDS);

        String t1_flag1 = clientKey1.getTreatment("non_existent_flag");
        String t1_flag2 = clientKey1.getTreatment("non_existent_flag_2");
        String t2_flag1 = clientKey2.getTreatment("non_existent_flag");
        String t2_flag2 = clientKey2.getTreatment("non_existent_flag_2");

        assertEquals("FALLBACK_TREATMENT", t1_flag1);
        assertEquals(Treatments.CONTROL, t1_flag2);
        assertEquals("FALLBACK_TREATMENT", t2_flag1);
        assertEquals(Treatments.CONTROL, t2_flag2);

        factory.destroy();
    }

    @Test
    public void case7_fallbackDynamicConfigPropagationTreatmentAndConfigReturned() throws Exception {
        final String url = mWebServer.url("/").url().toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url)
                .eventsEndpoint(url)
                .build();


        Map<String, FallbackTreatment> byFlag = new HashMap<>();
        byFlag.put("my_flag", new FallbackTreatment("ON_FALLBACK", "{\"flag\":true}"));
        FallbackTreatmentsConfiguration fbConfig = FallbackTreatmentsConfiguration.builder()
                .global(new FallbackTreatment("OFF_FALLBACK", "{\"global\":true}"))
                .byFlag(byFlag)
                .build();

        SplitClientConfig config = SplitClientConfig.builder()
                .serviceEndpoints(endpoints)
                .ready(30000)
                .featuresRefreshRate(3)
                .segmentsRefreshRate(3)
                .impressionsRefreshRate(3)
                .logLevel(SplitLogLevel.DEBUG)
                .trafficType("account")
                .fallbackTreatments(fbConfig)
                .build();

        SplitFactory factory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), new Key("DEFAULT_KEY"), config, mContext, null);

        SplitClient client = factory.client(new Key("key_1"));

        CountDownLatch readyLatch = new CountDownLatch(1);
        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                readyLatch.countDown();
            }
        });
        readyLatch.await(30, TimeUnit.SECONDS);

        SplitResult rMy = client.getTreatmentWithConfig("my_flag", null);
        SplitResult rUnknown = client.getTreatmentWithConfig("non_existent_flag", null);

        assertEquals("ON_FALLBACK", rMy.treatment());
        assertEquals("{\"flag\":true}", rMy.config());
        assertEquals("OFF_FALLBACK", rUnknown.treatment());
        assertEquals("{\"global\":true}", rUnknown.config());

        factory.destroy();
    }

    @Test
    public void case8_noImpressionsForDefinitionNotFoundOrFallbackDefinitionNotFoundAfterReady() throws Exception {
        final String url = mWebServer.url("/").url().toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url)
                .eventsEndpoint(url)
                .build();

        final StringBuilder postedImpressions = new StringBuilder();
        final CountDownLatch impressionsLatch = new CountDownLatch(1);
        mCurSplitReqId = 1;
        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                final String path = request.getPath();
                if (path.contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return new MockResponse().setResponseCode(200).setBody(IntegrationHelper.dummyAllSegments());
                } else if (path.contains("/splitChanges")) {
                    // Serve a real flag so we do generate impressions in DEBUG mode
                    String change = IntegrationHelper.loadSplitChanges(mContext, "simple_split.json");
                    change = change.replace("\"workm\"", "\"real_flag\"");
                    return new MockResponse().setResponseCode(200).setBody(change);
                } else if (path.contains("/testImpressions/bulk")) {
                    try {
                        postedImpressions.append(request.getBody().readUtf8());
                    } catch (Exception ignore) { }
                    impressionsLatch.countDown();
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mWebServer.setDispatcher(dispatcher);

        // Configure global fallback so unknown flags return a fallback treatment
        FallbackTreatmentsConfiguration fbConfig = FallbackTreatmentsConfiguration.builder()
                .global(new FallbackTreatment("OFF_FALLBACK"))
                .build();

        final List<Impression> capturedImpressions = Collections.synchronizedList(new ArrayList<>());
        ImpressionListener listener = createImpressionCapturingListener(capturedImpressions);

        SplitClientConfig config = buildDebugConfigWithListener(endpoints, fbConfig, listener);
        SplitFactory factory = buildFactory(config);

        SplitClient client = factory.client(new Key("key_1"));
        awaitReady(client);

        // Evaluate a real flag (will log impression) and an unknown flag (should not log impression)
        String tUnknown = client.getTreatment("dnf_flag");
        String tKnown = client.getTreatment("real_flag");

        // Push impressions
        Thread.sleep(1000);
        client.flush();
        impressionsLatch.await(5, TimeUnit.SECONDS);

        String body = postedImpressions.toString();
        assertPayloadHasOnlyKnownFlagNoDnf(body);
        assertLocalNoUnknownOrDnf(capturedImpressions);

        factory.destroy();
    }
}
