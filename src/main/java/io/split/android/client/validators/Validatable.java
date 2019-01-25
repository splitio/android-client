package io.split.android.client.validators;

public interface Validatable<T> {
    Boolean isValid(Validator<T> validator);
}
