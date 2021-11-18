package io.split.android.client.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Json {

    private static final Gson _json = new GsonBuilder().serializeNulls().create();

    public static String toJson(Object obj) {
        return _json.toJson(obj);
    }

    public static <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
        return _json.fromJson(json, typeOfT);
    }

    public static <T> List<T> fromJsonList(String json, Class<T> clz) throws JsonSyntaxException {
        Type listType = new TypeToken<ArrayList<T>>(){}.getType();
        return new Gson().fromJson(json, listType);

    }

    public static <T> T fromJson(String json, Class<T> clz) throws JsonSyntaxException {
        return _json.fromJson(json, clz);
    }

    public static Map<String, Object> genericValueMapFromJson(String json, Type attributesMapType) {
        Map<String, Object> map = _json.fromJson(json, attributesMapType);

        Set<Map.Entry<String, Object>> entries = map.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            if (entry.getValue() instanceof Double && ((Double) entry.getValue() % 1 == 0)) {
                entry.setValue(((Double) entry.getValue()).intValue());
            }
        }
        return map;
    }
}
