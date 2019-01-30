package io.split.android.client.validators;

/**
 * Interface to implement to create a
 * logger for validations components
 */
interface ValidationMessageLogger {
    void e(String message);
    void w(String message);
}
