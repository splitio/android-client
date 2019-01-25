package io.split.android.client.validators;

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
     *
     * @return a boolean indication if entity is valid
     */
    boolean isValidEntity(T entity);
}