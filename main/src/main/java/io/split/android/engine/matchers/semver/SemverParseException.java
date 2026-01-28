package io.split.android.engine.matchers.semver;

public class SemverParseException extends Exception {

    public SemverParseException(String message) {
        super(message);
    }

    public SemverParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
