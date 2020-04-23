package helper;

import android.content.Context;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import io.split.android.client.utils.Logger;

public class IntegrationHelper {
    public static final int NEVER_REFRESH_RATE = 999999;

    private final static Type EVENT_LIST_TYPE = new TypeToken<List<Event>>(){}.getType();
    private final static Type IMPRESSIONS_LIST_TYPE = new TypeToken<List<TestImpressions>>(){}.getType();
    private final static Gson mGson = new GsonBuilder().create();

    public static List<Event> buildEventsFromJson(String attributesJson) {

        List<Event> events;
        try {
            events = mGson.fromJson(attributesJson, EVENT_LIST_TYPE);
        } catch (Exception e) {
            events =  Collections.emptyList();
        }

        return events;
    }

    public static List<TestImpressions> buildImpressionsFromJson(String attributesJson) {

        List<TestImpressions> impressions;
        try {
            impressions = mGson.fromJson(attributesJson, IMPRESSIONS_LIST_TYPE);
        } catch (Exception e) {
            impressions =  Collections.emptyList();
        }

        return impressions;
    }

    public static boolean isEven(int i) {
        return (i % 2) == 0;
    }

    public static void logSeparator(String tag) {
        Log.i(tag, Strings.repeat("-", 200) );
    }

    public static String emptySplitChanges(long since, long till) {
        return String.format("{\"splits\":[], \"since\": %d, \"till\": %d }", since, till);
    }

    public static SplitFactory buidFactory(String apiToken, Key key, SplitClientConfig config,
                                           Context context, HttpClient httpClient) {
        Constructor[] c = SplitFactoryImpl.class.getDeclaredConstructors();
        Constructor constructor =c [1];
        constructor.setAccessible(true);
        SplitFactory factory = null;
        try {
            factory = (SplitFactory) constructor.newInstance(
                    apiToken, key, config, context, httpClient);
        } catch (Exception e) {
            Logger.e("Error creating factory: " + e.getLocalizedMessage());
        }
        return factory;
    }

    public static String dummyMySegments() {
        return "{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}";
    }

    public static String dummyApiKey() {
        return "99049fd8653247c5ea42bc3c1ae2c6a42bc3";
    }

    public static Key dummyUserKey() {
        return new Key("CUSTOMER_ID");
    }

    public static SplitClientConfig basicConfig() {
        return SplitClientConfig.builder()
                .ready(30000)
                .enableDebug()
                .trafficType("account")
                .build();
    }

    public static SplitClientConfig lowRefreshRateConfig() {
        TestableSplitConfigBuilder builder = new TestableSplitConfigBuilder()
                .ready(30000)
                .featuresRefreshRate(5)
                .segmentsRefreshRate(5)
                .impressionsRefreshRate(5)
                .impressionsChunkSize(999999)
                .enableDebug()
                .trafficType("account");
        return builder.build();
    }

}
