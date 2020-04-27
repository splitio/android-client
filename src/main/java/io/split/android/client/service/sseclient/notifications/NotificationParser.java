package io.split.android.client.service.sseclient.notifications;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;

import io.split.android.client.utils.Json;

import static io.split.android.client.service.sseclient.notifications.NotificationType.CONTROL;

public class NotificationParser {
    private final static String CONTROL_CHANNEL_TAG = "control";

    @NonNull
    public IncomingNotification parseIncoming(String jsonData) throws JsonSyntaxException {
        RawNotification rawNotification = Json.fromJson(jsonData, RawNotification.class);

        if (isControlChannel(rawNotification.getChannel())) {
            return new IncomingNotification(CONTROL, rawNotification.getChannel(), rawNotification.getData());
        }

        IncomingNotificationType type = Json.fromJson(rawNotification.getData(), IncomingNotificationType.class);
        return new IncomingNotification(type.getType(), rawNotification.getChannel(), rawNotification.getData());
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

    public ControlNotification parseControl(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, ControlNotification.class);
    }

    private boolean isControlChannel(String channel) {
        return channel != null && channel.contains(CONTROL_CHANNEL_TAG);
    }
}
