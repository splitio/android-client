package helper;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.TestImpressions;

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
}
