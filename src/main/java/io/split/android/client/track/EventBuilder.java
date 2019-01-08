package io.split.android.client.track;

import com.google.common.base.Strings;

import io.split.android.client.dtos.Event;
import io.split.android.client.utils.Logger;

public class EventBuilder {

    public class EventValidationException extends Throwable {
        public EventValidationException(String message) {
            super(message);
        }
    }

    private String matchingKey;
    private String type;
    private String trafficType;
    private double value;

    private final String EVENT_TYPE_VALIDATION_PATTERN = "^[a-zA-Z0-9][-_.:a-zA-Z0-9]{0,79}$";

    public EventBuilder setMatchingKey(String key) {
        this.matchingKey = key;
        return this;
    }

    public EventBuilder setType(String type) {
        this.type = type;
        return this;
    }

    public EventBuilder setTrafficType(String trafficType) {
        this.trafficType = trafficType;
        return this;
    }

    public EventBuilder setValue(double value) {
        this.value = value;
        return this;
    }

    private void validate() throws EventValidationException {

        if (this.type == null) {
            Logger.e("track event_type cannot be null");
            throw new EventValidationException("Event type null");
        }

        if (Strings.isNullOrEmpty(this.type)) {
            Logger.e("track: event_type must be not be an empty String");
            throw new EventValidationException("Event is empty");
        }

        if (!this.type.matches(EVENT_TYPE_VALIDATION_PATTERN)) {
            Logger.e("track: event name must adhere to the regular expression " + EVENT_TYPE_VALIDATION_PATTERN);
            throw new EventValidationException("Event is empty");
        }

        if (this.trafficType == null) {
            Logger.e("track: traffic_type_name cannot be null");
            throw new EventValidationException("Traffic type is null");
        }

        if (Strings.isNullOrEmpty(this.trafficType)) {
            Logger.e("Traffic Type was null or empty");
            throw new EventValidationException("Traffic type is empty");
        }

        if (this.matchingKey == null) {
            Logger.e("track: key cannot be null");
            throw new EventValidationException("Key is null");
        }

        if (Strings.isNullOrEmpty(this.matchingKey)) {
            Logger.e("track: key must be not be an empty String");
            throw new EventValidationException("Key is empty");
        }
    }

    public Event build() throws EventValidationException {

        validate();

        Event event = new Event();
        event.eventTypeId = type;
        event.trafficTypeName = trafficType;
        event.key = matchingKey;
        event.value = value;
        event.timestamp = System.currentTimeMillis();
        return event;
    }
}
