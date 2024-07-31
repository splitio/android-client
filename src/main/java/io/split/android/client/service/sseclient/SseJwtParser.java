package io.split.android.client.service.sseclient;

import androidx.annotation.Nullable;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class SseJwtParser {

    private static final String PUBLISHERS_CHANNEL_METADATA = "channel-metadata:publishers";
    private static final String PUBLISHERS_CHANNEL_PREFIX = "[?occupancy=metrics.publishers]";
    private static final String MY_LARGE_SEGMENTS_CHANNEL = "mylargesegments";

    static final Type ALL_TOKEN_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    private static final Type CHANNEL_TYPE = new TypeToken<Map<String, List<String>>>() {
    }.getType();

    public SseJwtToken parse(String rawToken, boolean largeSegmentsEnabled) throws InvalidJwtTokenException {

        if (rawToken == null) {
            Logger.e("Error: JWT is null.");
            throw new InvalidJwtTokenException();
        }

        final String encodedPayload = extractTokenData(rawToken);
        if (encodedPayload == null) {
            Logger.e("SSE authentication JWT payload is not valid.");
            throw new InvalidJwtTokenException();
        }

        String payload = Base64Util.decode(encodedPayload);
        if (payload == null) {
            Logger.e("Could not decode SSE authentication JWT payload.");
            throw new InvalidJwtTokenException();
        }

        Map<String, List<String>> channels = null;
        SseAuthToken authToken = null;
        try {
            authToken = Json.fromJson(payload, SseAuthToken.class);

            if (authToken == null || authToken.getChannelList() == null) {
                Logger.e("SSE JWT data is empty or not valid.");
                throw new InvalidJwtTokenException();
            }
            String unparsedChannels = authToken.getChannelList();
            channels = Json.fromJson(unparsedChannels, CHANNEL_TYPE);
            if (channels == null) {
                Logger.e("SSE JWT has not channels.");
                throw new InvalidJwtTokenException();
            }

        } catch (JsonSyntaxException e) {
            Logger.e("Error parsing SSE authentication JWT json " + e.getLocalizedMessage());
            throw new InvalidJwtTokenException();
        } catch (Exception e) {
            Logger.e("Unknown error while parsing SSE authentication JWT: " + e.getLocalizedMessage());
            throw new InvalidJwtTokenException();
        }

        List<String> processedChannels = new ArrayList<>();
        for (String channel : channels.keySet()) {
            boolean filterLargeSegmentsChannel = !largeSegmentsEnabled &&
                    channel.toLowerCase().contains(MY_LARGE_SEGMENTS_CHANNEL);
            if (!filterLargeSegmentsChannel) {
                List<String> channelInfo = channels.get(channel);
                if (channelInfo != null && channelInfo.contains(PUBLISHERS_CHANNEL_METADATA)) {
                    processedChannels.add(PUBLISHERS_CHANNEL_PREFIX + channel);
                } else {
                    processedChannels.add(channel);
                }
            }
        }

        return new SseJwtToken(authToken.getIssuedAt(), authToken.getExpirationAt(), processedChannels, rawToken);
    }

    @Nullable
    private String extractTokenData(String token) {
        String[] components = token.split("\\.");
        if (components.length > 1) {
            return components[1];
        }
        return null;
    }

    private List<String> emptyChannelList() {
        return new ArrayList<>();
    }
}
