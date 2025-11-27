package io.harness.events;

/**
 * No-op implementation of {@link Logging} for use when logging is not provided.
 */
final class NoOpLogging implements Logging {

    static final Logging INSTANCE = new NoOpLogging();

    private NoOpLogging() {}

    @Override
    public void logError(String message) {}

    @Override
    public void logWarning(String message) {}

    @Override
    public void logInfo(String message) {}

    @Override
    public void logDebug(String message) {}

    @Override
    public void logVerbose(String message) {}
}

