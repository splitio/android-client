package io.split.android.client.validators;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.api.Key;
import io.split.android.client.dtos.Event;

/**
 * Contains func an instance of Event class.
 */
public class EventValidatorImpl implements EventValidator {

    private ValidationMessageLogger mMessageLogger;
    private final String TYPE_REGEX = ValidationConfig.getInstance().getTrackEventNamePattern();
    private KeyValidator mKeyValidator;
    private String mTag = "";

    public EventValidatorImpl(String tag) {
        this.mMessageLogger = new ValidationMessageLoggerImpl(tag);
        this.mKeyValidator = new KeyValidatorImpl();
        this.mTag = tag;
    }

    @Override
    public boolean isValidEvent(Event event) {

        if(event == null) {
            return false;
        }

        if(!mKeyValidator.isValidKey(event.key, null, mTag)){
            return false;
        }

        if (event.trafficTypeName == null) {
            mMessageLogger.e("you passed a null or undefined traffic_type_name, traffic_type_name must be a non-empty string");
            return false;
        }

        if (Strings.isNullOrEmpty(event.trafficTypeName)) {
            mMessageLogger.e("you passed an empty traffic_type_name, traffic_type_name must be a non-empty string");
            return false;
        }

        if (event.eventTypeId == null) {
            mMessageLogger.e("you passed a null or undefined event_type, event_type must be a non-empty String");
            return false;
        }

        if (Strings.isNullOrEmpty(event.eventTypeId)) {
            mMessageLogger.e("you passed an empty event_type, event_type must be a non-empty String");
            return false;
        }

        if (!event.eventTypeId.matches(TYPE_REGEX)) {
            mMessageLogger.e("you passed " + (event.eventTypeId == null ? "null" : event.eventTypeId)
            + ", event name must adhere to the regular expression " + TYPE_REGEX
                    + ". This means an event name must be alphanumeric, cannot be more than 80 characters long, and can only include a dash, "
                    + " underscore, period, or colon as separators of alphanumeric characters.");
            return false;
        }

        return true;
    }

    @Override
    public boolean trafficTypeHasUppercaseLetters(Event event) {
        if(event == null) {
            return false;
        }
        return !event.trafficTypeName.toLowerCase().equals(event.trafficTypeName);
    }

    @Override
    public void setMessageLogger(ValidationMessageLogger logger) {
        this.mMessageLogger = logger;
    }

}