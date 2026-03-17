package io.split.android.client.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.split.android.client.tracker.TrafficTypeValidator;
import io.split.android.client.tracker.TrackerEventValidator;
import io.split.android.client.tracker.TrackerValidationError;

/**
 * Event validator implementation for the tracker module.
 */
public class EventValidatorImpl implements TrackerEventValidator {

    private final String TYPE_REGEX = ValidationConfig.getInstance().getTrackEventNamePattern();
    private KeyValidator mKeyValidator;
    private final TrafficTypeValidator mTrafficTypeValidator;

    public EventValidatorImpl(KeyValidator keyValidator, TrafficTypeValidator trafficTypeValidator) {
        mKeyValidator = keyValidator;
        mTrafficTypeValidator = trafficTypeValidator;
    }

    @Override
    public TrackerValidationError validate(String key, String trafficTypeName, String eventTypeId,
                                           Double value, Map<String, Object> properties, boolean isSdkReady) {
        ValidationErrorInfo errorInfo = mKeyValidator.validate(key, null);
        if(errorInfo != null){
            return new TrackerValidationError(true, errorInfo.getErrorMessage());
        }

        if (trafficTypeName == null) {
            return new TrackerValidationError(true, "you passed a null or undefined traffic_type_name, traffic_type_name must be a non-empty string");
        }

        if (ValidationUtils.isNullOrEmpty(trafficTypeName.trim())) {
            return new TrackerValidationError(true, "you passed an empty traffic_type_name, traffic_type_name must be a non-empty string");
        }

        if (eventTypeId == null) {
            return new TrackerValidationError(true, "you passed a null or undefined event_type, event_type must be a non-empty String");
        }

        if (ValidationUtils.isNullOrEmpty(eventTypeId.trim())) {
            return new TrackerValidationError(true, "you passed an empty event_type, event_type must be a non-empty String");
        }

        if (!eventTypeId.matches(TYPE_REGEX)) {
            return new TrackerValidationError(true, "you passed " + eventTypeId
            + ", event name must adhere to the regular expression " + TYPE_REGEX
                    + ". This means an event name must be alphanumeric, cannot be more than 80 characters long, and can only include a dash, "
                    + " underscore, period, or colon as separators of alphanumeric characters.");
        }

        List<String> warnings = new ArrayList<>();

        if(!trafficTypeName.toLowerCase().equals(trafficTypeName)) {
            warnings.add("traffic_type_name should be all lowercase - converting string to lowercase");
        }

        if (isSdkReady && !mTrafficTypeValidator.isValid(trafficTypeName)) {
            String message = "Traffic Type " + trafficTypeName + " does not have any corresponding feature flags in this environment, "
                    + "make sure you’re tracking your events to a valid traffic type defined in the Split user interface";
            warnings.add(message);
        }

        if (warnings.isEmpty()) {
            return null;
        }
        return new TrackerValidationError(warnings);
    }
}
