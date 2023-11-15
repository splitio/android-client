package io.split.android.client.utils;

import com.google.common.base.Strings;


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
}
