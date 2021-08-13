package io.split.android.client.service.sseclient.notifications;

import androidx.annotation.NonNull;

import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

import io.split.android.client.exceptions.MySegmentsParsingException;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.StringHelper;

import static io.split.android.client.service.sseclient.notifications.NotificationType.ERROR;
import static io.split.android.client.service.sseclient.notifications.NotificationType.OCCUPANCY;

public class NotificationParser {
    private final static Type BOUNDED_MAP_TYPE = new TypeToken<Map<Long, Byte>>(){}.getType();
    private final static String EVENT_TYPE_ERROR = "error";
    private static final String EVENT_TYPE_FIELD = "event";

    @NonNull
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
            if(type == null) {
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
    public SplitKillNotification parseSplitKill(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, SplitKillNotification.class);
    }

    public MySegmentChangeNotification parseMySegmentUpdate(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, MySegmentChangeNotification.class);
    }

    public MySegmentChangeV2Notification parseMySegmentUpdateV2(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, MySegmentChangeV2Notification.class);
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
}
