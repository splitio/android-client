package io.split.android.client.service.impressions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import io.split.android.client.dtos.KeyImpression;

public class KeyImpressionSerializer implements JsonSerializer<KeyImpression> {

    private final Gson mGson;

    public KeyImpressionSerializer() {
        mGson = new GsonBuilder()
                .serializeNulls()
                .create();
    }

    @Override
    public JsonElement serialize(KeyImpression src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = (JsonObject) mGson.toJsonTree(src);

        // If properties is null, remove it from the JSON object
        if (src.properties == null) {
            jsonObject.remove("properties");
        }

        return jsonObject;
    }
}
