package io.split.android.client.service.sseclient.notifications;

import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class StreamingMessageParser {
    private static final String EVENT_TYPE_ERROR = "error";
    private static final String EVENT_TYPE_FIELD = "event";
    private static final String EVENT_DATA_FIELD = "data";

    public @Nullable StreamingError parseError(Map<String, String> values) {
        if (!isError(values) || values.get(EVENT_DATA_FIELD) == null) {
            return null;
        }
        try {
            return Json.fromJson(values.get(EVENT_DATA_FIELD), StreamingError.class);
        } catch (JsonSyntaxException e) {
            Logger.e("Unknown error while parsing streaming error token: " + e.getLocalizedMessage());
            return null;
        }
    }

    public boolean isError(Map<String, String> values) {
        return values != null && EVENT_TYPE_ERROR.equals(values.get(EVENT_TYPE_FIELD));
    }
}
