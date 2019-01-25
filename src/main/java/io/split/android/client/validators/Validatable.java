package io.split.android.client.validators;

/**
 * This interface must be implemented to make an object suitable to validate
 * using an implementation of Validator interface
 */
public interface Validatable<T extends Validatable> {

    /**
     * @param validator The validator to validate the current instance.
     * @return a boolean indication if instance is valid
     */
    Boolean isValid(Validator<T> validator);
}
