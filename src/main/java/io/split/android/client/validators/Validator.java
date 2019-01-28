package io.split.android.client.validators;

import java.util.List;

/**
 * Interface to implement when creating an Entity validator
 *
 * This validation is implementing using the visitor pattern. Entities to be
 * validated should implement the Validatable interface, then any Validator implementation
 * should "Visit" entity through method isValid(validator) from Validatable and validate it.
 */
public interface Validator<T extends Validatable> {

    /**
     * Validates an entity. Entity should implement Validatable interface
     * validation should be short circuit, so it should return false when
     * the first error happens
     *
     * @return a boolean indication if entity is valid
     */
    boolean isValidEntity(T entity);

    /**
     * Allows to set a message logger intended to log validation messages
     *
     * @param: A logger implementing ValidationMessageLogger interface
     *
     * @return Void
     */
    public void setMessageLogger(ValidationMessageLogger logger);

    /**
     * Returns an integer representing the validation error that has occurred
     * If no error by convention the value should be 0
     *
     * @param: A logger implementing ValidationMessageLogger interface
     *
     * @return Void
     */
    public int getError();

    /**
     * List of warnings occurred while validation
     * is_valid response should be still true
     *
     * @return List to warnings occurred
     */
    public List<Integer> getWarnings();
}