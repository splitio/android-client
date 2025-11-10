package android.util;

/**
 * android.util.Base64 is not available for unit tests.
 *
 * We can't use java.util.Base64 due to our minSdk, however for unit tests it's fine.
 */
public class Base64 {

    public static String encodeToString(byte[] input, int flags) {
        return java.util.Base64.getEncoder().encodeToString(input);
    }
}
