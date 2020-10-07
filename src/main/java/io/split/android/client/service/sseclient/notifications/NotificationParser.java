package io.split.android.client.service.sseclient.notifications;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;

import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

import static io.split.android.client.service.sseclient.notifications.NotificationType.ERROR;
import static io.split.android.client.service.sseclient.notifications.NotificationType.OCCUPANCY;

public class NotificationParser {
    private final static String NAME_ERROR = "error";

    @NonNull
    public IncomingNotification parseIncoming(String jsonData) throws JsonSyntaxException {
        NotificationType type;
        RawNotification rawNotification = null;
        try {
            rawNotification = Json.fromJson(jsonData, RawNotification.class);
            if(isError(rawNotification)) {
                return new IncomingNotification(ERROR, "", "", 0L);
            }
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

    public OccupancyNotification parseOccupancy(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, OccupancyNotification.class);
    }

    public ControlNotification parseControl(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, ControlNotification.class);
    }

    public StreamingError parseError(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, StreamingError.class);
    }

    private boolean isError(RawNotification rawNotification) {
        return NAME_ERROR.equals(rawNotification.getName());
    }
}
