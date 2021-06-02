package io.split.android.client.service.impressions;

import androidx.annotation.NonNull;

import io.split.android.client.service.http.HttpRequestBodySerializer;
import io.split.android.client.utils.Json;

public class ImpressionsCountRequestBodySerializer implements HttpRequestBodySerializer<ImpressionsCount> {

    public String serialize(@NonNull ImpressionsCount data) {
        return Json.toJson(data);
    }
}
