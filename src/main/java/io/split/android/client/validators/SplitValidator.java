package io.split.android.client.validators;

import com.google.common.base.Strings;

/**
 * Interface to implement by Split validators
 */
public interface SplitValidator {

    /**
     * Checks that Split name follow validation rules
     * @param name: Split name
     * @return true when name is valid, false when it is not
     */
    public boolean isValidName(String name, String logTag);

    /**
     * Trims the name if it has leading/trailing whitespaces
     * @param name: Split name
     * @return true when name contains t , false when it is not
     */
    public String trimName(String name, String logTag);

    /**
     * Overrides de default message logger
     * @param logger: An implementation of ValidationMessageLogger
     *
     */
    public void setMessageLogger(ValidationMessageLogger logger);

}
