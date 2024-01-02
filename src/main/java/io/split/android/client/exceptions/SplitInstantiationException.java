package io.split.android.client.exceptions;

public class SplitInstantiationException extends Exception {

    public SplitInstantiationException(String message) {
        super(message);
    }

    public SplitInstantiationException(String message, Exception ex) {
        super(message, ex);
    }
}
