package io.split.android.engine.experiments;

class UnsupportedMatcherException extends Exception {

    UnsupportedMatcherException(String message) {
        super(message);
    }

    UnsupportedMatcherException(String message, Throwable cause) {
        super(message, cause);
    }
}
