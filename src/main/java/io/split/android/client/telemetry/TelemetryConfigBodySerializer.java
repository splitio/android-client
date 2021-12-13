package io.split.android.client.telemetry;

import androidx.annotation.NonNull;

import io.split.android.client.service.http.HttpRequestBodySerializer;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.utils.Json;

public class TelemetryConfigBodySerializer implements HttpRequestBodySerializer<Config> {

    @Override
    public String serialize(@NonNull Config data) {
        return Json.toJson(data);
    }
}
