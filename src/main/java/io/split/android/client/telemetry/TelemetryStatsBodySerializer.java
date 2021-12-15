package io.split.android.client.telemetry;

import androidx.annotation.NonNull;

import io.split.android.client.service.http.HttpRequestBodySerializer;
import io.split.android.client.telemetry.model.Stats;
import io.split.android.client.utils.Json;

public class TelemetryStatsBodySerializer implements HttpRequestBodySerializer<Stats> {

    @Override
    public String serialize(@NonNull Stats data) {
        return Json.toJson(data);
    }
}
