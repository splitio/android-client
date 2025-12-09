package io.split.android.client.events;

import io.split.android.client.SplitClient;
import io.split.android.client.api.EventMetadata;

/**
 * Base class for handling Split SDK events.
 * <p>
 * Extend this class and override the methods you need to handle specific SDK events.
 * You can implement both the metadata-enabled and versions of the methods;
 * if both are implemented, both will be called (metadata version first).
 * <p>
 * <b>Threading:</b>
 * <ul>
 *   <li>{@code onPostExecution} methods are called on a background thread (faster, executed immediately)</li>
 *   <li>{@code onPostExecutionView} methods are called on the main/UI thread (queued on main looper)</li>
 * </ul>
 * <p>
 * <b>Metadata:</b>
 * <ul>
 *   <li>Metadata-enabled methods receive {@link EventMetadata} containing event-specific information</li>
 *   <li>Metadata may be {@code null} for some events</li>
 *   <li>If you only need metadata, implement the metadata version; if you need backward compatibility,
 *       implement both versions</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * client.on(SplitEvent.SDK_UPDATE, new SplitEventTask() {
 *     @Override
 *     public void onPostExecution(SplitClient client, EventMetadata metadata) {
 *         List<String> updatedFlags = (List<String>) metadata.get("updatedFlags");
 *         // Handle update with metadata
 *     }
 *
 *     @Override
 *     public void onPostExecution(SplitClient client) {
 *         // Legacy handling (also called if both are implemented)
 *     }
 * });
 * }</pre>
 */
public class SplitEventTask {
    /**
     * Called when an event occurs, executed on a background thread.
     * <p>
     * Override this method to handle events on a background thread without metadata.
     * This method is executed immediately and is faster than {@link #onPostExecutionView(SplitClient)}.
     *
     * @param client the Split client instance
     * @throws SplitEventTaskMethodNotImplementedException if not overridden (default behavior)
     */
    public void onPostExecution(SplitClient client) {
        throw new SplitEventTaskMethodNotImplementedException();
    }

    /**
     * Called when an event occurs, executed on the main/UI thread.
     * <p>
     * Override this method to handle events on the main thread without metadata.
     * Use this when you need to update UI components.
     * <p>
     * Note: This method is queued on the main looper, so execution may be delayed
     * compared to {@link #onPostExecution(SplitClient)}.
     *
     * @param client the Split client instance
     * @throws SplitEventTaskMethodNotImplementedException if not overridden (default behavior)
     */
    public void onPostExecutionView(SplitClient client) {
        throw new SplitEventTaskMethodNotImplementedException();
    }

    /**
     * Called when an event occurs with metadata, executed on a background thread.
     * <p>
     * Override this method to handle events on a background thread with access to event metadata.
     * The metadata contains event-specific information such as updated flag names for SDK_UPDATE events.
     * This method is executed immediately and is faster than {@link #onPostExecutionView(SplitClient, EventMetadata)}.
     * <p>
     * If both this method and {@link #onPostExecution(SplitClient)} are implemented,
     * both will be called (this method first).
     *
     * @param client   the Split client instance
     * @param metadata the event metadata, may be {@code null} for some events
     * @throws SplitEventTaskMethodNotImplementedException if not overridden (default behavior)
     */
    public void onPostExecution(SplitClient client, EventMetadata metadata) {
        throw new SplitEventTaskMethodNotImplementedException();
    }

    /**
     * Called when an event occurs with metadata, executed on the main/UI thread.
     * <p>
     * Override this method to handle events on the main thread with access to event metadata.
     * The metadata contains event-specific information such as updated flag names for SDK_UPDATE events.
     * Use this when you need to update UI components based on event metadata.
     * <p>
     * Note: This method is queued on the main looper, so execution may be delayed
     * compared to {@link #onPostExecution(SplitClient, EventMetadata)}.
     * <p>
     * If both this method and {@link #onPostExecutionView(SplitClient)} are implemented,
     * both will be called (this method first).
     *
     * @param client   the Split client instance
     * @param metadata the event metadata, may be {@code null} for some events
     * @throws SplitEventTaskMethodNotImplementedException if not overridden (default behavior)
     */
    public void onPostExecutionView(SplitClient client, EventMetadata metadata) {
        throw new SplitEventTaskMethodNotImplementedException();
    }
}
