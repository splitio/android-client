package io.split.android.client.service.impressions.unique;

import androidx.annotation.NonNull;

import io.split.android.client.service.http.HttpRequestBodySerializer;
import io.split.android.client.utils.Json;

public class MTKRequestBodySerializer implements HttpRequestBodySerializer<MTK> {

    @Override
    public String serialize(@NonNull MTK data) {
        return Json.toJson(data);
    }
}
