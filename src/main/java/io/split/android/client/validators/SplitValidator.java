package io.split.android.client.validators;

import com.google.common.base.Strings;

/**
 * Interface to implement by Split validators
 */
public interface SplitValidator {

    /**
     * Checks that Split name follow validation rules
     * @param name: Split name
     * @return ValidationErrorInfo with code and message when name is not valid, null when if valid
     */
    public ValidationErrorInfo validateName(String name);

    /**
     * Builds the message to log when split is not found
     * @param splitName: Split name
     * @return message to log
     */
    public String splitNotFoundMessage(String splitName);


}
