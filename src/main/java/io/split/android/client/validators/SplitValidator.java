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
    public ValidationErrorInfo validateName(String name);

}
