package io.split.android.client.service.events;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.SerializedEvent;
import io.split.android.client.service.http.HttpRequestBodySerializer;
import io.split.android.client.utils.Json;

public class EventsRequestBodySerializer implements HttpRequestBodySerializer<List<Event>> {

    public String serialize(@NonNull List<Event> inputData) {
        List<SerializedEvent> data = new ArrayList<>();

        for (Event event : inputData) {
            data.add(new SerializedEvent(event));
        }

        return Json.toJson(data);
    }
}
