package helper;

import android.content.Context;

import androidx.core.util.Pair;

import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryImpl;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.network.HttpClient;
import io.split.android.client.service.synchronizer.SynchronizerSpy;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Logger;

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
        return String.format("{\"splits\":[], \"since\": %d, \"till\": %d }", since, till);
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
        Constructor[] c = SplitFactoryImpl.class.getDeclaredConstructors();
        Constructor constructor = c[1];
        constructor.setAccessible(true);
        SplitFactory factory = null;
        try {
            factory = (SplitFactory) constructor.newInstance(
                    apiToken, key, config, context, httpClient, database, synchronizerSpy);
        } catch (Exception e) {
            Logger.e("Error creating factory: " + e.getLocalizedMessage());
        }
        return factory;
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
                .enableDebug()
                .trafficType("account")
                .build();
    }

    public static SplitClientConfig lowRefreshRateConfig() {
        return lowRefreshRateConfig(true);
    }

    public static SplitClientConfig lowRefreshRateConfig(boolean streamingEnabled) {
        TestableSplitConfigBuilder builder = new TestableSplitConfigBuilder()
                .ready(30000)
                .featuresRefreshRate(3)
                .segmentsRefreshRate(3)
                .impressionsRefreshRate(3)
                .impressionsChunkSize(999999)
                .streamingEnabled(streamingEnabled)
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

}
