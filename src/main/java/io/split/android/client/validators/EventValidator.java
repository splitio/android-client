package io.split.android.client.validators;

import io.split.android.client.dtos.Event;

/**
 * Interface to implement by Track Events validators
 */
public interface EventValidator {

    /**
     * Checks that a Track event is valid
     * @param event: Event instance
     * @return true when the key is valid, false when it is not
     */
    public boolean isValidEvent(Event event);

    /**
     * Checks if TrafficTypeName has uppercase letters
     * @param event: An instance of Event class
     * @return true when the key is valid, false when it is not
     */
    public boolean trafficTypeHasUppercaseLetters(Event event);

    /**
     * Overrides de default message logger
     * @param logger: An implementation of ValidationMessageLogger
     *
     */
    public void setMessageLogger(ValidationMessageLogger logger);
}
