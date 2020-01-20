package io.split.android.client.service.events;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.service.HttpRequestBodySerializer;
import io.split.android.client.utils.Json;

public class EventsRequestBodySerializer implements HttpRequestBodySerializer<List<Event>> {
    public String serialize(@NonNull List<Event> data) {
        return Json.toJson(data);
    }
}
