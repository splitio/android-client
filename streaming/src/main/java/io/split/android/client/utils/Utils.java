package io.split.android.client.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Utility methods for the streaming module.
 */
public class Utils {

    private Utils() {
        // Utility class
    }

    public static <T> T checkNotNull(T obj) {
        return Objects.requireNonNull(obj);
    }

    public static <T> T checkNotNull(@Nullable T reference, @Nullable Object errorMessage) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }
        return reference;
    }

    public static boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.isEmpty();
    }

    @NonNull
    public static <T> T getOrDefault(@Nullable T value, @NonNull T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
