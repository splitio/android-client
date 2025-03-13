package io.split.android.client.service.sseclient.notifications;

import static io.split.android.client.service.sseclient.notifications.NotificationType.OCCUPANCY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonSyntaxException;

import java.util.Map;

import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class NotificationParser {
    private final static String EVENT_TYPE_ERROR = "error";
    private static final String EVENT_TYPE_FIELD = "event";

    public IncomingNotification parseIncoming(String jsonData) throws JsonSyntaxException {
        NotificationType type;
        RawNotification rawNotification = null;
        try {
            rawNotification = Json.fromJson(jsonData, RawNotification.class);
        } catch (JsonSyntaxException e) {
            Logger.e("Unexpected error while parsing raw notification: " + e.getLocalizedMessage());
            return null;
        }

        try {
            IncomingNotificationType notificationType
                    = Json.fromJson(rawNotification.getData(), IncomingNotificationType.class);
            type = notificationType.getType();
            if (type == null) {
                type = OCCUPANCY;
            }
        } catch (JsonSyntaxException e) {
            Logger.e("Error parsing notification: " + e.getLocalizedMessage());
            return null;
        } catch (Exception e) {
            Logger.e("Unexpected error while parsing incomming notification: " + e.getLocalizedMessage());
            return null;
        }

        return new IncomingNotification(type, rawNotification.getChannel(),
                rawNotification.getData(), rawNotification.getTimestamp());
    }

    @NonNull
    public SplitsChangeNotification parseSplitUpdate(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, SplitsChangeNotification.class);
    }

    @NonNull
    public RuleBasedSegmentChangeNotification parseRuleBasedSegmentUpdate(String notificationJson) {
        return Json.fromJson(notificationJson, RuleBasedSegmentChangeNotification.class);
    }

    @NonNull
    public SplitKillNotification parseSplitKill(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, SplitKillNotification.class);
    }

    public OccupancyNotification parseOccupancy(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, OccupancyNotification.class);
    }

    public ControlNotification parseControl(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, ControlNotification.class);
    }

    public StreamingError parseError(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, StreamingError.class);
    }

    public KeyList parseKeyList(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, KeyList.class);
    }

    public boolean isError(Map<String, String> values) {
        return values != null && EVENT_TYPE_ERROR.equals(values.get(EVENT_TYPE_FIELD));
    }

    @Nullable
    public String extractUserKeyHashFromChannel(String channel) {
        if (channel != null) {
            String[] channelSegments = channel.split("_");
            if (channelSegments.length > 2) {
                return channelSegments[2];
            }
        }

        return null;
    }

    @Nullable
    public MembershipNotification parseMembershipNotification(String jsonData) {
        try {
            return Json.fromJson(jsonData, MembershipNotification.class);
        } catch (Exception e) {
            Logger.w("Failed to parse membership notification");
            return null;
        }
    }
}
