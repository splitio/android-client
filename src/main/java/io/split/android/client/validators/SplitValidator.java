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
    public boolean isValidName(final String name);

    /**
     * Checks that Split name doesn't have leading and/or trailing spaces
     * @param name: Split name
     * @return true when name contains t , false when it is not
     */
    public boolean nameHasToBeTrimmed(String name);

    /**
     * Overrides de default message logger
     * @param logger: An implementation of ValidationMessageLogger
     *
     */
    public void setMessageLogger(ValidationMessageLogger logger);

    /**
     * Sets the tag displayed in logs
     * @param tag: String tag
     *
     */
    public void setTag(String tag);
}
