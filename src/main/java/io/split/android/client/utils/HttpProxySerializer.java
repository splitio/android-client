package io.split.android.client.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.dtos.HttpProxyDto;
import io.split.android.client.network.HttpProxy;
import io.split.android.client.storage.general.GeneralInfoStorage;

/**
 * Utility class for serializing and deserializing HttpProxy objects.
 */
public class HttpProxySerializer {

    private HttpProxySerializer() {
    }

    @Nullable
    public static String serialize(@Nullable HttpProxy httpProxy) {
        if (httpProxy == null) {
            return null;
        }
        HttpProxyDto dto = new HttpProxyDto(httpProxy);
        return Json.toJson(dto);
    }

    public static void serializeAndStore(@Nullable HttpProxy httpProxy, @NonNull GeneralInfoStorage storage) {
        String jsonProxy = serialize(httpProxy);
        storage.setProxyConfig(jsonProxy);
    }

    @Nullable
    public static HttpProxyDto deserialize(@Nullable String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return Json.fromJson(json, HttpProxyDto.class);
        } catch (Exception e) {
            return null;
        }
    }
}
