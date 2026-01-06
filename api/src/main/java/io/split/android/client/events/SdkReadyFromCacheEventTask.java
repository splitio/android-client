package io.split.android.client.events;

import io.split.android.client.SplitClient;

/**
 * Typed event task for SDK_READY_FROM_CACHE events.
 * <p>
 * Extend this class and override the typed methods to handle SDK_READY_FROM_CACHE events
 * with type-safe metadata access.
 * <p>
 * Example usage:
 * <pre>{@code
 * client.on(SdkEvent.SDK_READY_FROM_CACHE, new SdkReadyFromCacheEventTask() {
 *     @Override
 *     public void onPostExecution(SplitClient client, SdkReadyFromCacheMetadata metadata) {
 *         Boolean freshInstall = metadata.isFreshInstall();
 *         Long timestamp = metadata.getLastUpdateTimestamp();
 *         // Handle cache ready event
 *     }
 * });
 * }</pre>
 */
public class SdkReadyFromCacheEventTask extends SplitEventTask {

    /**
     * Called when SDK_READY_FROM_CACHE event occurs, executed on a background thread.
     * <p>
     * Override this method to handle SDK_READY_FROM_CACHE events with typed metadata.
     *
     * @param client   the Split client instance
     * @param metadata the typed metadata containing cache information
     * @throws SplitEventTaskMethodNotImplementedException if not overridden (default behavior)
     */
    public void onPostExecution(SplitClient client, SdkReadyFromCacheMetadata metadata) {
        throw new SplitEventTaskMethodNotImplementedException();
    }

    /**
     * Called when SDK_READY_FROM_CACHE event occurs, executed on the main/UI thread.
     * <p>
     * Override this method to handle SDK_READY_FROM_CACHE events with typed metadata on the main thread.
     *
     * @param client   the Split client instance
     * @param metadata the typed metadata containing cache information
     * @throws SplitEventTaskMethodNotImplementedException if not overridden (default behavior)
     */
    public void onPostExecutionView(SplitClient client, SdkReadyFromCacheMetadata metadata) {
        throw new SplitEventTaskMethodNotImplementedException();
    }
}
