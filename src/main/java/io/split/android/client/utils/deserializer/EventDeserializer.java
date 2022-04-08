package io.split.android.client.utils.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.split.android.client.dtos.Event;

public class EventDeserializer implements JsonDeserializer<Event> {

    @Override
    public Event deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonObject properties = (!jsonObject.get(Event.PROPERTIES_FIELD).isJsonNull()) ? jsonObject.get(Event.PROPERTIES_FIELD).getAsJsonObject() : new JsonObject();

        Event event = new Event();

        if (jsonObject.get(Event.SIZE_IN_BYTES_FIELD) != null && !jsonObject.get(Event.SIZE_IN_BYTES_FIELD).isJsonNull()) {
            event.setSizeInBytes(jsonObject.get(Event.SIZE_IN_BYTES_FIELD).getAsInt());
        }
        event.eventTypeId = jsonObject.get(Event.EVENT_TYPE_FIELD).getAsString();
        event.trafficTypeName = jsonObject.get(Event.TRAFFIC_TYPE_NAME_FIELD).getAsString();
        event.key = jsonObject.get(Event.KEY_FIELD).getAsString();
        event.value = jsonObject.get(Event.VALUE_FIELD).getAsDouble();
        event.timestamp = jsonObject.get(Event.TIMESTAMP_FIELD).getAsLong();
        event.properties = buildMappedProperties(properties);

        return event;
    }

    private static Map<String, Object> buildMappedProperties(JsonObject properties) {
        Map<String, Object> mappedProperties = new HashMap<>();

        if (properties == null) {
            return Collections.unmodifiableMap(mappedProperties);
        }

        for (Map.Entry<String, JsonElement> prop : properties.entrySet()) {
            JsonElement value = prop.getValue();
            String key = prop.getKey();

            if (value != null && !value.isJsonNull()) {
                try {
                    String valueAsString = value.getAsString();

                    if (valueAsString.equals(String.valueOf(value.getAsBoolean()))) {
                        mappedProperties.put(key, value.getAsBoolean());
                    } else if (valueAsString.equals(String.valueOf(value.getAsInt()))) {
                        mappedProperties.put(key, value.getAsInt());
                    } else if (valueAsString.equals(String.valueOf(value.getAsLong()))) {
                        mappedProperties.put(key, value.getAsLong());
                    } else if (valueAsString.equals(String.valueOf(value.getAsDouble()))) {
                        mappedProperties.put(key, value.getAsDouble());
                    } else if (valueAsString.equals(String.valueOf(value.getAsBigDecimal()))) {
                        mappedProperties.put(key, value.getAsBigDecimal());
                    } else {
                        mappedProperties.put(key, valueAsString);
                    }
                } catch (NumberFormatException numberFormatException) {
                    mappedProperties.put(key, value.getAsString());
                }
            } else {
                mappedProperties.put(key, null);
            }
        }

        return Collections.unmodifiableMap(mappedProperties);
    }
}
