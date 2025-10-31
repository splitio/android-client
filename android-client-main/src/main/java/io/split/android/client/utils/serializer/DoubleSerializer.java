package io.split.android.client.utils.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class DoubleSerializer implements JsonSerializer<Double> {

    @Override
    public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == src.longValue())
            return new JsonPrimitive(src.longValue());
        return new JsonPrimitive(src);
    }
}
