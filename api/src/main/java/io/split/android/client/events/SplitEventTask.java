package io.split.android.client.events;

import io.split.android.client.SplitClient;

/**
 * Base class for handling Split SDK events.
 * <p>
 * Extend this class and override the methods you need to handle specific SDK events.
 * <p>
 * <b>Threading:</b>
 * <ul>
 *   <li>{@code onPostExecution} methods are called on a background thread (faster, executed immediately)</li>
 *   <li>{@code onPostExecutionView} methods are called on the main/UI thread (queued on main looper)</li>
 * </ul>
 * <p>
 * For events with metadata (like SDK_UPDATE or SDK_READY_FROM_CACHE), use
 * {@link SdkEventListener} instead for type-safe metadata access.
 * <p>
 * Example usage:
 * <pre>{@code
 * client.on(SplitEvent.SDK_READY, new SplitEventTask() {
 *     @Override
 *     public void onPostExecution(SplitClient client) {
 *         // SDK is ready, start using Split
 *     }
 * });
 * }</pre>
 */
public class SplitEventTask {
    /**
     * Called when an event occurs, executed on a background thread.
     * <p>
     * Override this method to handle events on a background thread.
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
     * Override this method to handle events on the main thread.
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
}
