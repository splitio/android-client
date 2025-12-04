package io.split.android.client.events;

import io.harness.events.Logging;
import io.split.android.client.utils.logger.Logger;

/**
 * Implementation of {@link Logging} that delegates to the Split SDK {@link Logger}.
 */
public class SplitLogging implements Logging {

    @Override
    public void logError(String message) {
        Logger.e(message);
    }

    @Override
    public void logWarning(String message) {
        Logger.w(message);
    }

    @Override
    public void logInfo(String message) {
        Logger.i(message);
    }

    @Override
    public void logDebug(String message) {
        Logger.d(message);
    }

    @Override
    public void logVerbose(String message) {
        Logger.v(message);
    }
}
