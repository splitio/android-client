package io.split.android.client.service;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.utils.Json;

public class EventsRequestParser implements HttpRequestParser<List<Event>> {
    public String parse(@NonNull List<Event> data) {
        return Json.toJson(data);
    }
}
