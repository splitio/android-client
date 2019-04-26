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

    public EventValidatorImpl(KeyValidator keyValidator) {
        this.mKeyValidator = keyValidator;
    }

    @Override
    public ValidationErrorInfo validate(Event event) {

        if(event == null) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "Event could not be null");
        }

        ValidationErrorInfo errorInfo = mKeyValidator.validate(event.key, null);
        if(errorInfo != null){
            return errorInfo;
        }

        if (event.trafficTypeName == null) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed a null or undefined traffic_type_name, traffic_type_name must be a non-empty string");
        }

        if (Strings.isNullOrEmpty(event.trafficTypeName.trim())) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed an empty traffic_type_name, traffic_type_name must be a non-empty string");
        }

        if (event.eventTypeId == null) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed a null or undefined event_type, event_type must be a non-empty String");
        }

        if (Strings.isNullOrEmpty(event.eventTypeId.trim())) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed an empty event_type, event_type must be a non-empty String");
        }

        if (!event.eventTypeId.matches(TYPE_REGEX)) {
            return new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "you passed " + event.eventTypeId
            + ", event name must adhere to the regular expression " + TYPE_REGEX
                    + ". This means an event name must be alphanumeric, cannot be more than 80 characters long, and can only include a dash, "
                    + " underscore, period, or colon as separators of alphanumeric characters.");
        }

        if(!event.trafficTypeName.toLowerCase().equals(event.trafficTypeName)) {
            return new ValidationErrorInfo(ValidationErrorInfo.WARNING_TRAFFIC_TYPE_HAS_UPPERCASE_CHARS, "traffic_type_name should be all lowercase - converting string to lowercase", true);
        }

        return null;
    }
}