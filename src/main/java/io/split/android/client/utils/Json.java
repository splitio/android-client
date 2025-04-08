package io.split.android.client.utils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.SplitFactoryImpl;
import io.split.android.client.utils.serializer.DoubleSerializer;

public class Json {

    private static final Gson mJson = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(Double.class, new DoubleSerializer())
            .create();
    private static volatile Gson mNonNullJson;

    public static String toJson(Object obj) {
        return mJson.toJson(obj);
    }

    public static String toJsonIgnoringNulls(Object obj) {
        return getNonNullsGsonInstance().toJson(obj);
    }

    public static <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
        return mJson.fromJson(json, typeOfT);
    }

    public static <T> List<T> fromJsonList(String json, Class<T> clz) throws JsonSyntaxException {
        Type listType = new TypeToken<ArrayList<T>>(){}.getType();
        return new Gson().fromJson(json, listType);

    }

    public static <T> T fromJson(String json, Class<T> clz) throws JsonSyntaxException {
        return mJson.fromJson(json, clz);
    }

    @NonNull
    public static Map<String, Object> genericValueMapFromJson(String json, Type attributesMapType) {
        Map<String, Object> map = mJson.fromJson(json, attributesMapType);

        Set<Map.Entry<String, Object>> entries = map.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            if (entry.getValue() instanceof Double && ((Double) entry.getValue() % 1 == 0)) {
                entry.setValue(((Double) entry.getValue()).intValue());
            }
        }
        return map;
    }

    private static Gson getNonNullsGsonInstance() {
        if (mNonNullJson == null) {
            synchronized (Json.class) {
                if (mNonNullJson == null) {
                    mNonNullJson = new GsonBuilder()
                            .registerTypeAdapter(Double.class, new DoubleSerializer())
                            .create();
                }
            }
        }

        return mNonNullJson;
    }

    /**
     * Efficiently extracts sets and trafficTypeName fields from a JSON string.
     * Uses a lightweight approach for better performance during SDK initialization.
     *
     * @param json The JSON string to parse
     * @return A SplitFieldsResult containing the sets and trafficTypeName
     */
    public static SplitFieldsResult extractSplitFields(String json) {
        if (json == null || json.isEmpty()) {
            return new SplitFieldsResult(Collections.emptySet(), null);
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Use a more targeted approach with JsonParser
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            
            // Extract sets - only if present
            Set<String> sets = Collections.emptySet();
            if (jsonObject.has("sets") && !jsonObject.get("sets").isJsonNull()) {
                JsonArray setsArray = jsonObject.getAsJsonArray("sets");
                if (setsArray.size() > 0) {
                    sets = new HashSet<>(setsArray.size());
                    for (JsonElement element : setsArray) {
                        if (element.isJsonPrimitive()) {
                            sets.add(element.getAsString());
                        }
                    }
                }
            }
            
            // Extract trafficTypeName - only if present
            String trafficTypeName = null;
            if (jsonObject.has("trafficTypeName") && !jsonObject.get("trafficTypeName").isJsonNull() && 
                jsonObject.get("trafficTypeName").isJsonPrimitive()) {
                trafficTypeName = jsonObject.get("trafficTypeName").getAsString();
            }
            
            return new SplitFieldsResult(sets, trafficTypeName);
        } catch (Exception e) {
            // Return empty results in case of parsing error
            return new SplitFieldsResult(Collections.emptySet(), null);
        }
    }

    /**
     * Result class for the extractSplitFields method
     */
    public static class SplitFieldsResult {
        private final Set<String> sets;
        private final String trafficTypeName;
        
        public SplitFieldsResult(Set<String> sets, String trafficTypeName) {
            this.sets = sets;
            this.trafficTypeName = trafficTypeName;
        }
        
        public Set<String> getSets() {
            return sets;
        }
        
        public String getTrafficTypeName() {
            return trafficTypeName;
        }
    }
}
