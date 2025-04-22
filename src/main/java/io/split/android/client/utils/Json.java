package io.split.android.client.utils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

}
