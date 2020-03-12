package io.split.android.client.service.sseclient;

import android.util.Base64;

import androidx.annotation.Nullable;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class SseChannelsParser {

    final static String CHANNEL_LIST_FIELD = "x-ably-capability";

    final static Type ALL_TOKEN_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    final static Type CHANNEL_TYPE = new TypeToken<Map<String, List<String>>>() {
    }.getType();

    public List<String> parse(String token) {
        final String encodedPayload = extractTokenData(token);
        if (encodedPayload == null) {
            return emptyChannelList();
        }

        String payload = base64Decode(encodedPayload);
        if (payload == null) {
            return emptyChannelList();
        }

        Map<String, List<String>> channels = null;
        try {
            Map<String, Object> allToken = Json.fromJson(payload, ALL_TOKEN_TYPE);

            if (allToken == null) {
                return emptyChannelList();
            }
            String unparsedChannels = allToken.get(CHANNEL_LIST_FIELD).toString();
            channels = Json.fromJson(unparsedChannels, CHANNEL_TYPE);
            if (channels == null) {
                return emptyChannelList();
            }
        } catch (JsonSyntaxException e) {
            Logger.e("Error parsing json token " + e.getLocalizedMessage());
            return emptyChannelList();
        }
        return new ArrayList<>(channels.keySet());
    }

    @Nullable
    private String extractTokenData(String token) {
        String[] components = token.split("\\.");
        if (components.length > 1) {
            return components[1];
        }
        return null;
    }

    @Nullable
    private String base64Decode(String string) {
        String decoded = null;
        try {
            byte[] bytes = Base64.decode(string, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            decoded = new String(bytes, Charset.defaultCharset());
        } catch (IllegalArgumentException e) {
            Logger.e("Received bytes didn't correspond to a valid Base64 encoded string." + e.getLocalizedMessage());
        }
        return decoded;
    }

    private List<String> emptyChannelList() {
        return new ArrayList<>();
    }
}
