package io.split.android.client.events;

import io.split.android.client.SplitClient;

/**
 * Typed event task for SDK_UPDATE events.
 * <p>
 * Extend this class and override the typed methods to handle SDK_UPDATE events
 * with type-safe metadata access.
 * <p>
 * Example usage:
 * <pre>{@code
 * client.on(SdkEvent.SDK_UPDATE, new SdkUpdateEventTask() {
 *     @Override
 *     public void onPostExecution(SplitClient client, SdkUpdateMetadata metadata) {
 *         List<String> flags = metadata.getUpdatedFlags();
 *         // Handle updated flags
 *     }
 * });
 * }</pre>
 */
public class SdkUpdateEventTask extends SplitEventTask {

    /**
     * Called when SDK_UPDATE event occurs, executed on a background thread.
     * <p>
     * Override this method to handle SDK_UPDATE events with typed metadata.
     *
     * @param client   the Split client instance
     * @param metadata the typed metadata containing updated flag information
     * @throws SplitEventTaskMethodNotImplementedException if not overridden (default behavior)
     */
    public void onPostExecution(SplitClient client, SdkUpdateMetadata metadata) {
        throw new SplitEventTaskMethodNotImplementedException();
    }

    /**
     * Called when SDK_UPDATE event occurs, executed on the main/UI thread.
     * <p>
     * Override this method to handle SDK_UPDATE events with typed metadata on the main thread.
     *
     * @param client   the Split client instance
     * @param metadata the typed metadata containing updated flag information
     * @throws SplitEventTaskMethodNotImplementedException if not overridden (default behavior)
     */
    public void onPostExecutionView(SplitClient client, SdkUpdateMetadata metadata) {
        throw new SplitEventTaskMethodNotImplementedException();
    }
}
