package helper;

import android.content.Context;

import androidx.core.util.Pair;

import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryImpl;
import io.split.android.client.TestingConfig;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.lifecycle.SplitLifecycleManager;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.service.synchronizer.SynchronizerSpy;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.client.utils.logger.SplitLogLevel;
import io.split.android.client.utils.logger.Logger;
import fake.HttpStreamResponseMock;

public class IntegrationHelper {
    public static final int NEVER_REFRESH_RATE = 999999;

    private final static Type EVENT_LIST_TYPE = new TypeToken<List<Event>>() {
    }.getType();
    private final static Type IMPRESSIONS_LIST_TYPE = new TypeToken<List<TestImpressions>>() {
    }.getType();
    private final static Gson mGson = new GsonBuilder().create();

    public static List<Event> buildEventsFromJson(String attributesJson) {

        List<Event> events;
        try {
            events = mGson.fromJson(attributesJson, EVENT_LIST_TYPE);
        } catch (Exception e) {
            events = Collections.emptyList();
        }

        return events;
    }

    public static List<TestImpressions> buildImpressionsFromJson(String attributesJson) {

        List<TestImpressions> impressions;
        try {
            impressions = mGson.fromJson(attributesJson, IMPRESSIONS_LIST_TYPE);
        } catch (Exception e) {
            impressions = Collections.emptyList();
        }

        return impressions;
    }

    public static boolean isEven(int i) {
        return (i % 2) == 0;
    }

    public static void logSeparator(String tag) {
        Logger.i(tag, Strings.repeat("-", 200));
    }

    public static String emptySplitChanges(long since, long till) {
        return emptySplitChanges(till);
    }

    public static String emptySplitChanges(long till) {
        return String.format("{\"splits\":[], \"since\": %d, \"till\": %d }", till, till);
    }

    public static SplitFactory buildFactory(String apiToken, Key key, SplitClientConfig config,
                                            Context context, HttpClient httpClient) {
        return buildFactory(apiToken, key, config, context, httpClient, null, null);
    }

    public static SplitFactory buildFactory(String apiToken, Key key, SplitClientConfig config,
                                            Context context, HttpClient httpClient, SplitRoomDatabase database) {
        return buildFactory(apiToken, key, config, context, httpClient, database, null);
    }

    public static SplitFactory buildFactory(String apiToken, Key key, SplitClientConfig config,
                                            Context context, HttpClient httpClient, SplitRoomDatabase database,
                                            SynchronizerSpy synchronizerSpy) {
        return buildFactory(apiToken, key, config, context, httpClient, database, synchronizerSpy, null);
    }

    public static SplitFactory buildFactory(String apiToken, Key key, SplitClientConfig config,
                                            Context context, HttpClient httpClient, SplitRoomDatabase database,
                                            SynchronizerSpy synchronizerSpy, NetworkHelper networkHelper) {
        return buildFactory(apiToken, key, config, context, httpClient, database, synchronizerSpy, networkHelper, null);
    }

    public static SplitFactory buildFactory(String apiToken, Key key, SplitClientConfig config,
                                            Context context, HttpClient httpClient, SplitRoomDatabase database,
                                            SynchronizerSpy synchronizerSpy, NetworkHelper networkHelper, TestingConfig testingConfig) {
        return buildFactory(apiToken, key, config, context, httpClient, database,
                synchronizerSpy, networkHelper, testingConfig, null);
    }

    public static SplitFactory buildFactory(String apiToken, Key key, SplitClientConfig config,
                                            Context context, HttpClient httpClient, SplitRoomDatabase database,
                                            SynchronizerSpy synchronizerSpy, NetworkHelper networkHelper, TestingConfig testingConfig,
                                            SplitLifecycleManager lifecycleManager) {
        return buildFactory(apiToken, key, config, context, httpClient, database,
                synchronizerSpy, networkHelper, testingConfig, lifecycleManager, null);
    }

    public static SplitFactory buildFactory(String apiToken, Key key, SplitClientConfig config,
                                            Context context, HttpClient httpClient, SplitRoomDatabase database,
                                            SynchronizerSpy synchronizerSpy, NetworkHelper networkHelper,
                                            TestingConfig testingConfig, SplitLifecycleManager lifecycleManager,
                                            TelemetryStorage telemetryStorage) {
        Constructor[] c = SplitFactoryImpl.class.getDeclaredConstructors();
        Constructor constructor = c[1];
        constructor.setAccessible(true);
        SplitFactory factory = null;
        try {
            factory = (SplitFactory) constructor.newInstance(
                    apiToken, key, config, context, httpClient, database, synchronizerSpy, selectNetworkHelper(networkHelper),
                    testingConfig, lifecycleManager, telemetryStorage);
        } catch (Exception e) {
            Logger.e("Error creating factory: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return factory;
    }

    private static NetworkHelper selectNetworkHelper(NetworkHelper networkHelper) {
        return networkHelper != null ? networkHelper : new NetworkHelperStub();
    }

    public static String dummyMySegments() {
        return "{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}";
    }

    public static String emptyMySegments() {
        return "{\"mySegments\":[]}";
    }

    public static String dummyApiKey() {
        return "99049fd8653247c5ea42bc3c1ae2c6a42bc3";
    }

    /***
     * Returns api key and its corresponding db name (encripted api key)
     * @return
     */
    public static Pair<String, String> dummyApiKeyAndDb() {
        return new Pair<>(dummyApiKey(),
                "2a1099049fd8653247c5ea42bOIajMRhH0R0FcBwJZM4ca7zj6HAq1ZDS");
    }

    public static Key dummyUserKey() {
        return new Key("CUSTOMER_ID");
    }

    public static SplitClientConfig basicConfig() {
        return SplitClientConfig.builder()
                .ready(30000)
                .streamingEnabled(true)
                .logLevel(SplitLogLevel.DEBUG)
                .trafficType("account")
                .build();
    }

    public static TestingConfig testingConfig(int cdnBackoffTime) {
        TestingConfig testingConfig = new TestingConfig();
        testingConfig.setCdnBackoffTime(cdnBackoffTime);
        return testingConfig;
    }

    public static SplitClientConfig lowRefreshRateConfig() {
        return lowRefreshRateConfig(true);
    }

    public static SplitClientConfig lowRefreshRateConfig(boolean streamingEnabled) {
        return lowRefreshRateConfig(streamingEnabled, false);
    }

    public static SplitClientConfig lowRefreshRateConfig(boolean streamingEnabled, boolean telemetryEnabled) {
        TestableSplitConfigBuilder builder = new TestableSplitConfigBuilder()
                .ready(30000)
                .featuresRefreshRate(3)
                .segmentsRefreshRate(3)
                .impressionsRefreshRate(3)
                .impressionsChunkSize(999999)
                .streamingEnabled(streamingEnabled)
                .shouldRecordTelemetry(telemetryEnabled)
                .enableDebug()
                .trafficType("account");
        return builder.build();
    }

    public static String streamingEnabledToken() {
        // This token expires in 2040
        return "{" +
                "    \"pushEnabled\": true," +
                "    \"connDelay\": 0," +
                "    \"token\": \"eyJhbGciOiJIUzI1NiIsImtpZCI6IjVZOU05US45QnJtR0EiLCJ0eXAiOiJKV1QifQ.eyJ4LWFibHktY2FwYWJpbGl0eSI6IntcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X01UY3dOVEkyTVRNME1nPT1fbXlTZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X3NwbGl0c1wiOltcInN1YnNjcmliZVwiXSxcImNvbnRyb2xfcHJpXCI6W1wic3Vic2NyaWJlXCIsXCJjaGFubmVsLW1ldGFkYXRhOnB1Ymxpc2hlcnNcIl0sXCJjb250cm9sX3NlY1wiOltcInN1YnNjcmliZVwiLFwiY2hhbm5lbC1tZXRhZGF0YTpwdWJsaXNoZXJzXCJdfSIsIngtYWJseS1jbGllbnRJZCI6ImNsaWVudElkIiwiZXhwIjoyMjA4OTg4ODAwLCJpYXQiOjE1ODc0MDQzODh9.LcKAXnkr-CiYVxZ7l38w9i98Y-BMAv9JlGP2i92nVQY\"" +
                "}";

    }

    public static String streamingDisabledToken() {
        return "{\"pushEnabled\": false }";
    }

    public static String streamingEnabledV1Token() {
        return "{\"connDelay\":0,\"pushEnabled\":true,\"token\":\"eyJhbGciOiJIUzI1NiIsImtpZCI6IjVZOU05US5pSGZUUmciLCJ0eXAiOiJKV1QifQ.eyJ4LWFibHktY2FwYWJpbGl0eSI6IntcIk56TTJNREk1TXpjMF9NVGd5TlRnMU1UZ3dOZz09X01Ua3pOamd3TURFNE1BPT1fbXlTZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk56TTJNREk1TXpjMF9NVGd5TlRnMU1UZ3dOZz09X01qWXhNRE0yTkRjd09RPT1fbXlTZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk56TTJNREk1TXpjMF9NVGd5TlRnMU1UZ3dOZz09X3NwbGl0c1wiOltcInN1YnNjcmliZVwiXSxcImNvbnRyb2xfcHJpXCI6W1wic3Vic2NyaWJlXCIsXCJjaGFubmVsLW1ldGFkYXRhOnB1Ymxpc2hlcnNcIl0sXCJjb250cm9sX3NlY1wiOltcInN1YnNjcmliZVwiLFwiY2hhbm5lbC1tZXRhZGF0YTpwdWJsaXNoZXJzXCJdfSIsIngtYWJseS1jbGllbnRJZCI6ImNsaWVudElkIiwiZXhwIjoxNjQ4NjU2MjU4LCJpYXQiOjE2NDg2NTI2NTh9.MWwudv3kafKr-gVeqt-ClLAkCngZsDhdWx-dwqM9rxs\"}";
    }

    /**
     * Builds a dispatcher with the given responses.
     *
     * @param responses The responses to be returned by the dispatcher. The keys are url paths.
     * @return The dispatcher to be used in {@link HttpClientMock}
     */
    public static HttpResponseMockDispatcher buildDispatcher(Map<String, ResponseClosure> responses) {
        return buildDispatcher(responses, Collections.emptyMap());
    }

    /**
     * Builds a dispatcher with the given responses.
     *
     * @param responses          The responses to be returned by the dispatcher. The keys are url paths.
     * @param streamingResponses The streaming responses to be returned by the dispatcher. The keys are url paths.
     * @return The dispatcher to be used in {@link HttpClientMock}
     */
    public static HttpResponseMockDispatcher buildDispatcher(Map<String, ResponseClosure> responses, Map<String, StreamingResponseClosure> streamingResponses) {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                String path = uri.getPath()
                        .replace("sdk.split.io/api", "")
                        .replace("telemetry.split.io/api", "")
                        .replace("/api/", "");

                Logger.d("path is %s", path);
                if (responses.containsKey(path)) {
                    return responses.get(path).onResponse(uri, method, body);
                } else {
                    return new HttpResponseMock(200, "{}");
                }
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    String path = uri.getPath().replace("/api/", "");
                    if (streamingResponses.containsKey(path)) {
                        return streamingResponses.get(path).onResponse(uri);
                    } else {
                        return new HttpStreamResponseMock(200, null);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }
        };
    }

    /**
     * A simple interface to allow us to define the response for a given path
     */
    public interface ResponseClosure {
        HttpResponseMock onResponse(URI uri,
                                    HttpMethod httpMethod,
                                    String body);
    }

    /**
     * A simple interface to allow us to define the streaming response for a given path
     */
    public interface StreamingResponseClosure {
        HttpStreamResponseMock onResponse(URI uri);
    }
}
