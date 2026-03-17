package io.split.android.client.validators;

import androidx.annotation.Nullable;

/**
 * Utility methods for validator implementations.
 * <p>
 * This class provides helper methods used by validators to avoid depending on
 * the main module's Utils class.
 */
public class ValidationUtils {

    /**
     * Checks if a string is null or empty.
     *
     * @param string the string to check
     * @return true if the string is null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.isEmpty();
    }

    private ValidationUtils() {
        // Utility class, prevent instantiation
    }
}
