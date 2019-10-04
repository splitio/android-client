import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.TestImpressions;

public class IntegrationHelper {
    public static final int NEVER_REFRESH_RATE = 999999;

    public static List<Event> buildEventsFromJson(String attributesJson) {

        GsonBuilder gsonBuilder = new GsonBuilder();

        Type mapType = new TypeToken<List<Event>>(){}.getType();
        Gson gson = gsonBuilder.create();
        List<Event> events;
        try {
            events = gson.fromJson(attributesJson, mapType);
        } catch (Exception e) {
            events =  Collections.emptyList();
        }

        return events;
    }

    public static List<TestImpressions> buildImpressionsFromJson(String attributesJson) {

        GsonBuilder gsonBuilder = new GsonBuilder();

        Type mapType = new TypeToken<List<TestImpressions>>(){}.getType();
        Gson gson = gsonBuilder.create();
        List<TestImpressions> impressions;
        try {
            impressions = gson.fromJson(attributesJson, mapType);
        } catch (Exception e) {
            impressions =  Collections.emptyList();
        }

        return impressions;
    }
}
