package io.harness.events;

/**
 * Interface for optional logging in the events module.
 * Consumers can implement this interface to receive diagnostic output.
 */
public interface Logging {

    void logError(String message);

    void logWarning(String message);

    void logInfo(String message);

    void logDebug(String message);

    void logVerbose(String message);
}
