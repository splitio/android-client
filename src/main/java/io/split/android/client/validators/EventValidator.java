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
    ValidationErrorInfo validate(Event event, boolean validateTrafficType);

}
