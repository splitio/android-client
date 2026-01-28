package io.split.android.client.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;

/**
 * JSON utility class for the streaming module.
 */
public class Json {

    private static final Gson mJson = new GsonBuilder()
            .serializeNulls()
            .create();

    public static String toJson(Object obj) {
        return mJson.toJson(obj);
    }

    public static <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
        return mJson.fromJson(json, typeOfT);
    }

    public static <T> T fromJson(String json, Class<T> clz) throws JsonSyntaxException {
        return mJson.fromJson(json, clz);
    }
}
