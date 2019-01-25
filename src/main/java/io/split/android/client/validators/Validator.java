package io.split.android.client.validators;

public interface Validator<T> {
    boolean isValidEntity(T entity);
}