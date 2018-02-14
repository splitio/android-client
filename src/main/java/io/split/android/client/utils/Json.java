package io.split.android.client.utils;

import com.google.gson.Gson;

import java.lang.reflect.Type;

public class Json {

    private static final Gson _json = new Gson();

    public static String toJson(Object obj) {
        return _json.toJson(obj);
    }

    public static <T> T fromJson(String json, Type typeOfT) {
        return _json.fromJson(json, typeOfT);
    }

    public static <T> T fromJson(String json, Class<T> clz) {
        return _json.fromJson(json, clz);
    }

}
