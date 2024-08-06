package helper;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import fake.HttpStreamResponseMock;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryImpl;
import io.split.android.client.TestingConfig;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.lifecycle.SplitLifecycleManager;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.service.synchronizer.SynchronizerSpy;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Utils;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;

public class IntegrationHelper {
    public static final int NEVER_REFRESH_RATE = 999999;

    private final static Type EVENT_LIST_TYPE = new TypeToken<List<Event>>() {
    }.getType();
    private final static Type IMPRESSIONS_LIST_TYPE = new TypeToken<List<TestImpressions>>() {
    }.getType();
    private final static Gson mGson = new GsonBuilder().create();
    public static final String JSON_SPLIT_WITH_TRAFFIC_TYPE_TEMPLATE = "{\"name\":\"%s\", \"changeNumber\": %d, \"trafficTypeName\":\"%s\", \"sets\":[\"%s\"]}";

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
        Logger.i(tag, Utils.repeat("-", 200));
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
                                            SynchronizerSpy synchronizerSpy, TestingConfig testingConfig) {
        return buildFactory(apiToken, key, config, context, httpClient, database,
                synchronizerSpy, testingConfig, null);
    }

    public static SplitFactory buildFactory(String apiToken, Key key, SplitClientConfig config,
                                            Context context, HttpClient httpClient, SplitRoomDatabase database,
                                            SynchronizerSpy synchronizerSpy, TestingConfig testingConfig,
                                            SplitLifecycleManager lifecycleManager) {
        return buildFactory(apiToken, key, config, context, httpClient, database,
                synchronizerSpy, testingConfig, lifecycleManager, null);
    }

    public static SplitFactory buildFactory(String apiToken, Key key, SplitClientConfig config,
                                            Context context, HttpClient httpClient, SplitRoomDatabase database,
                                            SynchronizerSpy synchronizerSpy,
                                            TestingConfig testingConfig, SplitLifecycleManager lifecycleManager,
                                            TelemetryStorage telemetryStorage) {
        if (testingConfig == null) {
            testingConfig = new TestingConfig();
            testingConfig.setFlagsSpec(null);
        }

        Constructor[] c = SplitFactoryImpl.class.getDeclaredConstructors();
        Constructor constructor = c[1];
        constructor.setAccessible(true);
        SplitFactory factory = null;
        try {
            factory = (SplitFactory) constructor.newInstance(
                    apiToken, key, config, context, httpClient, database, synchronizerSpy,
                    testingConfig, lifecycleManager, telemetryStorage);
        } catch (Exception e) {
            Logger.e("Error creating factory: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return factory;
    }

    public static String dummyMySegments() {
        return "{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}";
    }

    public static String dummyMyLargeSegments() {
        return "{\"myLargeSegments\":[\"large-segment1\", \"large-segment2\", \"large-segment3\"], \"till\": 9999999999999}";
    }

    public static String emptyMySegments() {
        return "{\"mySegments\":[]}";
    }

    public static String emptyMyLargeSegments() {
        return "{\"mySegments\":[], \"till\": 9999999999999}";
    }

    public static String randomizedMyLargeSegments() {
        int randIntOne = (int) (Math.random() * 100);
        int randIntTwo = (int) (Math.random() * 100);
        return "{\"myLargeSegments\":[\"large-segment1\", \"large-random\", \"large-segment"+randIntOne+"\", \"large-segment"+randIntTwo+"\"], \"till\": 9999999999999}";
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
        return lowRefreshRateConfig(streamingEnabled, false, true, 60L, 2L);
    }

    public static SplitClientConfig lowRefreshRateConfig(boolean streamingEnabled, boolean telemetryEnabled) {
        return lowRefreshRateConfig(streamingEnabled, telemetryEnabled, true, 60L, 2L);
    }

    public static SplitClientConfig syncDisabledConfig() {
        return lowRefreshRateConfig(true, false, false, 60L, 2L);
    }

    public static SplitClientConfig customSseConnectionDelayConfig(boolean streamingEnabled, long delay, long disconnectionDelay) {
        return lowRefreshRateConfig(streamingEnabled, false, true, delay, disconnectionDelay);
    }

    public static SplitClientConfig lowRefreshRateConfig(boolean streamingEnabled, boolean telemetryEnabled, boolean syncEnabled, long delay, long sseDisconnectionDelay) {
        TestableSplitConfigBuilder builder = new TestableSplitConfigBuilder()
                .ready(30000)
                .featuresRefreshRate(3)
                .segmentsRefreshRate(3)
                .impressionsRefreshRate(3)
                .impressionsChunkSize(999999)
                .syncEnabled(syncEnabled)
                .defaultSSEConnectionDelayInSecs(delay)
                .sseDisconnectionDelayInSecs(sseDisconnectionDelay)
                .streamingEnabled(streamingEnabled)
                .shouldRecordTelemetry(telemetryEnabled)
                .enableDebug()
                .trafficType("account");
        return builder.build();
    }

    public static String streamingEnabledToken() {
        return streamingEnabledToken(0);
    }

    public static String streamingEnabledToken(int delay) {
        // This token expires in 2040
        return "{" +
                "    \"pushEnabled\": true," +
                "    \"connDelay\": " + delay + "," +
                "    \"token\": \"eyJhbGciOiJIUzI1NiIsImtpZCI6IjVZOU05US45QnJtR0EiLCJ0eXAiOiJKV1QifQ.eyJ4LWFibHktY2FwYWJpbGl0eSI6IntcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X01UY3dOVEkyTVRNME1nPT1fbXlTZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X3NwbGl0c1wiOltcInN1YnNjcmliZVwiXSxcImNvbnRyb2xfcHJpXCI6W1wic3Vic2NyaWJlXCIsXCJjaGFubmVsLW1ldGFkYXRhOnB1Ymxpc2hlcnNcIl0sXCJjb250cm9sX3NlY1wiOltcInN1YnNjcmliZVwiLFwiY2hhbm5lbC1tZXRhZGF0YTpwdWJsaXNoZXJzXCJdfSIsIngtYWJseS1jbGllbnRJZCI6ImNsaWVudElkIiwiZXhwIjoyMjA4OTg4ODAwLCJpYXQiOjE1ODc0MDQzODh9.LcKAXnkr-CiYVxZ7l38w9i98Y-BMAv9JlGP2i92nVQY\"" +
                "}";

    }

    public static String streamingEnabledTokenLargeSegments() {
        return "{" +
                "    \"pushEnabled\": true," +
                "    \"connDelay\": " + 0 + "," +
                "    \"token\": \"eyJhbGciOiJIUzI1NiIsImtpZCI6IjVZOU05US45QnJtR0EiLCJ0eXAiOiJKV1QifQ.ewogICJ4LWFibHktY2FwYWJpbGl0eSI6ICJ7XCJNek01TmpjME9EY3lOZz09X01URXhNemd3TmpneF9NVGN3TlRJMk1UTTBNZz09X215U2VnbWVudHNcIjpbXCJzdWJzY3JpYmVcIl0sXCJNek01TmpjME9EY3lOZz09X01URXhNemd3TmpneF9NVGN3TlRJMk1UTTBNZz09X215bGFyZ2VzZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X3NwbGl0c1wiOltcInN1YnNjcmliZVwiXSxcImNvbnRyb2xfcHJpXCI6W1wic3Vic2NyaWJlXCIsXCJjaGFubmVsLW1ldGFkYXRhOnB1Ymxpc2hlcnNcIl0sXCJjb250cm9sX3NlY1wiOltcInN1YnNjcmliZVwiLFwiY2hhbm5lbC1tZXRhZGF0YTpwdWJsaXNoZXJzXCJdfSIsCiAgIngtYWJseS1jbGllbnRJZCI6ICJjbGllbnRJZCIsCiAgImV4cCI6IDIyMDg5ODg4MDAsCiAgImlhdCI6IDE1ODc0MDQzODgKfQ==.LcKAXnkr-CiYVxZ7l38w9i98Y-BMAv9JlGP2i92nVQY\"" +
                "}";
    }

    public static String streamingDisabledToken() {
        return "{\"pushEnabled\": false }";
    }

    public static String streamingEnabledV1Token() {
        return "{\"connDelay\":0,\"pushEnabled\":true,\"token\":\"eyJhbGciOiJIUzI1NiIsImtpZCI6IjVZOU05US5pSGZUUmciLCJ0eXAiOiJKV1QifQ.eyJ4LWFibHktY2FwYWJpbGl0eSI6IntcIk56TTJNREk1TXpjMF9NVGd5TlRnMU1UZ3dOZz09X01Ua3pOamd3TURFNE1BPT1fbXlTZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk56TTJNREk1TXpjMF9NVGd5TlRnMU1UZ3dOZz09X01qWXhNRE0yTkRjd09RPT1fbXlTZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk56TTJNREk1TXpjMF9NVGd5TlRnMU1UZ3dOZz09X3NwbGl0c1wiOltcInN1YnNjcmliZVwiXSxcImNvbnRyb2xfcHJpXCI6W1wic3Vic2NyaWJlXCIsXCJjaGFubmVsLW1ldGFkYXRhOnB1Ymxpc2hlcnNcIl0sXCJjb250cm9sX3NlY1wiOltcInN1YnNjcmliZVwiLFwiY2hhbm5lbC1tZXRhZGF0YTpwdWJsaXNoZXJzXCJdfSIsIngtYWJseS1jbGllbnRJZCI6ImNsaWVudElkIiwiZXhwIjoxNjQ4NjU2MjU4LCJpYXQiOjE2NDg2NTI2NTh9.MWwudv3kafKr-gVeqt-ClLAkCngZsDhdWx-dwqM9rxs\"}";
    }

    public static String splitChangeV2CompressionType2() {
        return splitChangeV2("9999999999999",
                "1000",
                "2",
                "eJzMk99u2kwQxV8lOtdryQZj8N6hD5QPlThSTVNVEUKDPYZt1jZar1OlyO9emf8lVFWv2ss5zJyd82O8hTWUZSqZvW04opwhUVdsIKBSSKR+10vS1HWW7pIdz2NyBjRwHS8IXEopTLgbQqDYT+ZUm3LxlV4J4mg81LpMyKqygPRc94YeM6eQTtjphp4fegLVXvD6Qdjt9wPXF6gs2bqCxPC/2eRpDIEXpXXblpGuWCDljGptZ4bJ5lxYSJRZBoFkTcWKozpfsoH0goHfCXpB6PfcngDpVQnZEUjKIlOr2uwWqiC3zU5L1aF+3p7LFhUkPv8/mY2nk3gGgZxssmZzb8p6A9n25ktVtA9iGI3ODXunQ3HDp+AVWT6F+rZWlrWq7MN+YkSWWvuTDvkMSnNV7J6oTdl6qKTEvGnmjcCGjL2IYC/ovPYgUKnvvPtbmrmApiVryLM7p2jE++AfH6fTx09/HvuF32LWnNjStM0Xh3c8ukZcsZlEi3h8/zCObsBpJ0acqYLTmFdtqitK1V6NzrfpdPBbLmVx4uK26e27izpDu/r5yf/16AXun2Cr4u6w591xw7+LfDidLj6Mv8TXwP8xbofv/c7UmtHMmx8BAAD//0fclvU=");
    }

    public static String splitChangeV2CompressionType1() {
        return splitChangeV2("9999999999999",
                "1000",
                "1",
                "H4sIAAAAAAAA/8yT327aTBDFXyU612vJxoTgvUMfKB8qcaSapqoihAZ7DNusvWi9TpUiv3tl/pdQVb1qL+cwc3bOj/EGzlKeq3T6tuaYCoZEXbGFgMogkXXDIM0y31v4C/aCgMnrU9/3gl7Pp4yilMMIAuVusqDamvlXeiWIg/FAa5OSU6aEDHz/ip4wZ5Be1AmjoBsFAtVOCO56UXh31/O7ApUjV1eQGPw3HT+NIPCitG7bctIVC2ScU63d1DK5gksHCZPnEEhXVC45rosFW8ig1++GYej3g85tJEB6aSA7Aqkpc7Ws7XahCnLTbLVM7evnzalsUUHi8//j6WgyTqYQKMilK7b31tRryLa3WKiyfRCDeHhq2Dntiys+JS/J8THUt5VyrFXlHnYTQ3LU2h91yGdQVqhy+0RtTeuhUoNZ08wagTVZdxbBndF5vYVApb7z9m9pZgKaFqwhT+6coRHvg398nEweP/157Bd+S1hz6oxtm88O73B0jbhgM47nyej+YRRfgdNODDlXJWcJL9tUF5SqnRqfbtPr4LdcTHnk4rfp3buLOkG7+Pmp++vRM9w/wVblzX7Pm8OGfxf5YDKZfxh9SS6B/2Pc9t/7ja01o5k1PwIAAP//uTipVskEAAA=");
    }

    public static String splitChangeV2CompressionType0() {
        return splitChangeV2("9999999999999",
                "1000",
                "0",
                "eyJ0cmFmZmljVHlwZU5hbWUiOiJ1c2VyIiwiaWQiOiJkNDMxY2RkMC1iMGJlLTExZWEtOGE4MC0xNjYwYWRhOWNlMzkiLCJuYW1lIjoibWF1cm9famF2YSIsInRyYWZmaWNBbGxvY2F0aW9uIjoxMDAsInRyYWZmaWNBbGxvY2F0aW9uU2VlZCI6LTkyMzkxNDkxLCJzZWVkIjotMTc2OTM3NzYwNCwic3RhdHVzIjoiQUNUSVZFIiwia2lsbGVkIjpmYWxzZSwiZGVmYXVsdFRyZWF0bWVudCI6Im9mZiIsImNoYW5nZU51bWJlciI6MTY4NDMyOTg1NDM4NSwiYWxnbyI6MiwiY29uZmlndXJhdGlvbnMiOnt9LCJjb25kaXRpb25zIjpbeyJjb25kaXRpb25UeXBlIjoiV0hJVEVMSVNUIiwibWF0Y2hlckdyb3VwIjp7ImNvbWJpbmVyIjoiQU5EIiwibWF0Y2hlcnMiOlt7Im1hdGNoZXJUeXBlIjoiV0hJVEVMSVNUIiwibmVnYXRlIjpmYWxzZSwid2hpdGVsaXN0TWF0Y2hlckRhdGEiOnsid2hpdGVsaXN0IjpbImFkbWluIiwibWF1cm8iLCJuaWNvIl19fV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvZmYiLCJzaXplIjoxMDB9XSwibGFiZWwiOiJ3aGl0ZWxpc3RlZCJ9LHsiY29uZGl0aW9uVHlwZSI6IlJPTExPVVQiLCJtYXRjaGVyR3JvdXAiOnsiY29tYmluZXIiOiJBTkQiLCJtYXRjaGVycyI6W3sia2V5U2VsZWN0b3IiOnsidHJhZmZpY1R5cGUiOiJ1c2VyIn0sIm1hdGNoZXJUeXBlIjoiSU5fU0VHTUVOVCIsIm5lZ2F0ZSI6ZmFsc2UsInVzZXJEZWZpbmVkU2VnbWVudE1hdGNoZXJEYXRhIjp7InNlZ21lbnROYW1lIjoibWF1ci0yIn19XX0sInBhcnRpdGlvbnMiOlt7InRyZWF0bWVudCI6Im9uIiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJvZmYiLCJzaXplIjoxMDB9LHsidHJlYXRtZW50IjoiVjQiLCJzaXplIjowfSx7InRyZWF0bWVudCI6InY1Iiwic2l6ZSI6MH1dLCJsYWJlbCI6ImluIHNlZ21lbnQgbWF1ci0yIn0seyJjb25kaXRpb25UeXBlIjoiUk9MTE9VVCIsIm1hdGNoZXJHcm91cCI6eyJjb21iaW5lciI6IkFORCIsIm1hdGNoZXJzIjpbeyJrZXlTZWxlY3RvciI6eyJ0cmFmZmljVHlwZSI6InVzZXIifSwibWF0Y2hlclR5cGUiOiJBTExfS0VZUyIsIm5lZ2F0ZSI6ZmFsc2V9XX0sInBhcnRpdGlvbnMiOlt7InRyZWF0bWVudCI6Im9uIiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJvZmYiLCJzaXplIjoxMDB9LHsidHJlYXRtZW50IjoiVjQiLCJzaXplIjowfSx7InRyZWF0bWVudCI6InY1Iiwic2l6ZSI6MH1dLCJsYWJlbCI6ImRlZmF1bHQgcnVsZSJ9XX0=");
    }

    public static String splitChangeV2(String changeNumber, String previousChangeNumber, String compressionType, String compressedPayload) {
        return "id: vQQ61wzBRO:0:0\n" +
                "event: message\n" +
                "data: {\"id\":\"m2T85LA4fQ:0:0\",\"clientId\":\"pri:NzIyNjY1MzI4\",\"timestamp\":"+System.currentTimeMillis()+",\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MTgyNTg1MTgwNg==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_UPDATE\\\",\\\"changeNumber\\\":"+changeNumber+",\\\"pcn\\\":"+previousChangeNumber+",\\\"c\\\":"+compressionType+",\\\"d\\\":\\\""+compressedPayload+"\\\"}\"}\n";
    }

    public static String splitKill(String changeNumber, String splitName) {
        return "id:cf74eb42-f687-48e4-ad18-af2125110aac\n" +
                "event:message\n" +
                "data:{\"id\":\"-OT-rGuSwz:0:0\",\"clientId\":\"NDEzMTY5Mzg0MA==:NDIxNjU0NTUyNw==\",\"timestamp\":"+System.currentTimeMillis()+",\"encoding\":\"json\",\"channel\":\"NzM2MDI5Mzc0_MTgyNTg1MTgwNg==_splits\",\"data\":\"{\\\"type\\\":\\\"SPLIT_KILL\\\",\\\"changeNumber\\\":" + changeNumber + ",\\\"defaultTreatment\\\":\\\"off\\\",\\\"splitName\\\":\\\"" + splitName + "\\\"}\"}\n";
    }

    public static String loadSplitChanges(Context context, String fileName) {
        FileHelper fileHelper = new FileHelper();
        String change = fileHelper.loadFileContent(context, fileName);
        SplitChange parsedChange = Json.fromJson(change, SplitChange.class);
        parsedChange.since = parsedChange.till;
        return Json.toJson(parsedChange);
    }

    /**
     * Builds a dispatcher with the given responses.
     *
     * @param responses The responses to be returned by the dispatcher. The keys are url paths.
     * @return The dispatcher to be used in {@link HttpClientMock}
     */
    public static HttpResponseMockDispatcher buildDispatcher(Map<String, ResponseClosure> responses) {
        return buildDispatcher(responses, null);
    }

    public static HttpResponseMockDispatcher buildDispatcher(Map<String, ResponseClosure> responses, @Nullable BlockingQueue<String> streamingQueue) {
        return buildDispatcher(responses, streamingQueue, null);
    }

    /**
     * Builds a dispatcher with the given responses.
     *
     * @param responses          The responses to be returned by the dispatcher. The keys are url paths.
     * @param streamingQueue The streaming responses to be returned by the dispatcher.
     * @return The dispatcher to be used in {@link HttpClientMock}
     */
    public static HttpResponseMockDispatcher buildDispatcher(Map<String, ResponseClosure> responses, @Nullable BlockingQueue<String> streamingQueue, CountDownLatch sseLatch) {
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
                    if (sseLatch != null) {
                        sseLatch.countDown();
                    }
                    return new HttpStreamResponseMock(200, streamingQueue);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
    }

    public static String sha256(byte[] encoded) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return Base64.encodeToString(digest.digest(encoded), Base64.NO_WRAP);
    }

    /**
     * A simple interface to allow us to define the response for a given path
     */
    public interface ResponseClosure {
        HttpResponseMock onResponse(URI uri,
                                    HttpMethod httpMethod,
                                    String body);

        static String getSinceFromUri(URI uri) {
            try {
                return parse(uri.getQuery()).get("since");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        static Map<String, String> parse(String query) throws UnsupportedEncodingException {
            Map<String, String> queryPairs = new HashMap<>();
            String[] pairs = query.split("&");

            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                try {
                    String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");

                    queryPairs.put(key, value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return queryPairs;
        }
    }

    /**
     * A simple interface to allow us to define the streaming response for a given path
     */
    public interface StreamingResponseClosure {
        HttpStreamResponseMock onResponse(URI uri);
    }
}
