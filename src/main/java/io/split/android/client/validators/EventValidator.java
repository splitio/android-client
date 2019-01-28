package io.split.android.client.validators;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.api.Key;
import io.split.android.client.dtos.Event;

/**
 * Validates an instance of Key class.
 */
public class EventValidator implements Validator<Event> {

    public final static int NO_ERROR = 0;
    public final static int ERROR_NULL_TYPE = 1;
    public final static int ERROR_EMPTY_TYPE = 2;
    public final static int ERROR_REGEX_TYPE = 3;
    public final static int ERROR_NULL_TRAFFIC_TYPE = 4;
    public final static int ERROR_EMPTY_TRAFFIC_TYPE = 5;
    public final static int ERROR_NULL_KEY = 6;
    public final static int ERROR_EMPTY_KEY = 7;
    public final static int ERROR_LONG_KEY = 8;

    public final static int WARNING_UPPERCASE_CHARS_IN_TRAFFIC_TYPE = 100;

    private int mError = EventValidator.NO_ERROR;
    private List<Integer> mWarnings = new ArrayList<>();

    private ValidationMessageLogger mMessageLogger;
    private final String TYPE_REGEX = ValidationConfig.getInstance().getTrackEventNamePattern();

    public EventValidator(String tag) {
        this.mMessageLogger = new ValidationMessageLoggerImpl(tag);
    }

    @Override
    public boolean isValidEntity(Event entity) {

        Key key = new Key(entity.key, null);
        KeyValidator keyValidator = new KeyValidator("");
        keyValidator.setMessageLogger(mMessageLogger);
        mError = EventValidator.NO_ERROR;
        mWarnings.clear();

        if (!key.isValid(keyValidator)){
            mError = mapKeyErrorToEventError(keyValidator.getError());
            return false;
        }

        if (entity.trafficTypeName == null) {
            mMessageLogger.e("you passed a null or undefined traffic_type_name, traffic_type_name must be a non-empty string");
            mError = ERROR_NULL_TRAFFIC_TYPE;
            return false;
        }

        if (Strings.isNullOrEmpty(entity.trafficTypeName)) {
            mMessageLogger.e("you passed an empty traffic_type_name, traffic_type_name must be a non-empty string");
            mError = ERROR_EMPTY_TRAFFIC_TYPE;
            return false;
        }

        if (entity.eventTypeId == null) {
            mMessageLogger.e("you passed a null or undefined event_type, event_type must be a non-empty String");
            mError = ERROR_NULL_TYPE;
            return false;
        }

        if (Strings.isNullOrEmpty(entity.eventTypeId)) {
            mMessageLogger.e("you passed an empty event_type, event_type must be a non-empty String");
            mError = ERROR_EMPTY_TYPE;
            return false;
        }

        if (!entity.eventTypeId.matches(TYPE_REGEX)) {
            mMessageLogger.e("you passed " + (entity.eventTypeId == null ? "null" : entity.eventTypeId)
            + ", event name must adhere to the regular expression " + TYPE_REGEX
                    + ". This means an event name must be alphanumeric, cannot be more than 80 characters long, and can only include a dash, "
                    + " underscore, period, or colon as separators of alphanumeric characters.");
            mError = ERROR_REGEX_TYPE;
            return false;
        }

        if (!entity.trafficTypeName.toLowerCase().equals(entity.trafficTypeName)) {
            mMessageLogger.w("traffic_type_name should be all lowercase - converting string to lowercase");
            mWarnings.add(new Integer(WARNING_UPPERCASE_CHARS_IN_TRAFFIC_TYPE));
        }

        return true;
    }


    public void setMessageLogger(ValidationMessageLogger logger) {
        this.mMessageLogger = logger;
    }

    public int getError() {
        return mError;
    }

    public List<Integer> getWarnings() {
        return mWarnings;
    }

    private int mapKeyErrorToEventError(int keyError) {
        int eventError = NO_ERROR;
        switch(keyError) {
            case KeyValidator.ERROR_NULL_MATCHING_KEY:
                eventError = ERROR_NULL_KEY;
                break;
            case KeyValidator.ERROR_EMPTY_MATCHING_KEY:
                eventError = ERROR_EMPTY_KEY;
                break;
            case KeyValidator.ERROR_LONG_MATCHING_KEY:
                eventError = ERROR_LONG_KEY;
                break;
        }
        return eventError;
    }
}
