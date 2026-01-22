package io.harness.events;

class TestLogging implements Logging {
    String errorMessage;
    String warningMessage;
    String infoMessage;
    String debugMessage;
    String verboseMessage;

    @Override
    public void logError(String message) {
        errorMessage = message;
    }

    @Override
    public void logWarning(String message) {
        warningMessage = message;
    }

    @Override
    public void logInfo(String message) {
        infoMessage = message;
    }

    @Override
    public void logDebug(String message) {
        debugMessage = message;
    }

    @Override
    public void logVerbose(String message) {
        verboseMessage = message;
    }
}
