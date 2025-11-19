package io.split.android.client.service.events;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.SerializableEvent;
import io.split.android.client.service.http.HttpRequestBodySerializer;
import io.split.android.client.utils.Json;

public class EventsRequestBodySerializer implements HttpRequestBodySerializer<List<Event>> {

    public String serialize(@NonNull List<Event> inputData) {
        List<SerializableEvent> data = new ArrayList<>();

        for (Event event : inputData) {
            SerializableEvent serializableEvent = new SerializableEvent();
            serializableEvent.eventTypeId = event.eventTypeId;
            serializableEvent.trafficTypeName = event.trafficTypeName;
            serializableEvent.key = event.key;
            serializableEvent.value = event.value;
            serializableEvent.timestamp = event.timestamp;
            serializableEvent.properties = event.properties;

            data.add(serializableEvent);
        }

        return Json.toJson(data);
    }
}
