package io.split.android.client.service.sseclient.notifications;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;

import io.split.android.client.utils.Json;

public class NotificationParser {

    @NonNull
    public RawNotification parseRawNotification(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, RawNotification.class);
    }

    @NonNull
    public IncomingNotification parseIncoming(String jsonData) throws JsonSyntaxException {
        return Json.fromJson(jsonData, IncomingNotification.class);
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
}