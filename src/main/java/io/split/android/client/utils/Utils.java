package io.split.android.client.utils;

import androidx.annotation.Nullable;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Utils {

    private static String sanitizeForFolderName(String string) {
        if(string == null) {
            return "";
        }
        return string.replaceAll("[^a-zA-Z0-9]", "");
    }

    public static String convertApiKeyToFolder(String apiKey) {
        final int SALT_LENGTH = 29;
        final String SALT_PREFIX = "$2a$10$";
        final String CHAR_TO_FILL_SALT = "A";
        String sanitizedApiKey = sanitizeForFolderName(apiKey);
        StringBuilder salt = new StringBuilder(SALT_PREFIX);
        if (sanitizedApiKey.length() >= SALT_LENGTH - SALT_PREFIX.length()) {
            salt.append(sanitizedApiKey.substring(0, SALT_LENGTH - SALT_PREFIX.length()));
        } else {
            salt.append(sanitizedApiKey);
            salt.append(Strings.repeat(CHAR_TO_FILL_SALT, (SALT_LENGTH - SALT_PREFIX.length()) - sanitizedApiKey.length()));
        }
        // Remove last end of strings
        String cleanedSalt = salt.toString().substring(0, 29);
        String hash = BCrypt.hashpw(sanitizedApiKey, cleanedSalt);

        return (hash != null ? sanitizeForFolderName(hash) : null);
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

    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    public static int getAsInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int) value;
        }
    }

    public static <T> List<List<T>> partition(List<T> list, int size) {
        if (list == null || size <= 0) {
            throw new IllegalArgumentException("List must not be null and size must be greater than 0");
        }

        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(new ArrayList<>(list.subList(i, Math.min(i + size, list.size()))));
        }

        return partitions;
    }

    public static <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
        Set<T> result = new HashSet<>(set1);
        result.retainAll(set2);
        return result;
    }

    public static boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.isEmpty();
    }
}
