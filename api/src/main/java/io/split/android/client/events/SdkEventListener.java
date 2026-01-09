package io.split.android.client.events;

import io.split.android.client.SplitClient;

/**
 * Abstract class for handling SDK events with typed metadata.
 * <p>
 * Extend this class and override the methods you need to handle specific SDK events.
 * Each event has two callback options:
 * <ul>
 *   <li>Background thread callbacks (e.g., {@link #onUpdate}) - executed immediately on a background thread</li>
 *   <li>Main thread callbacks (e.g., {@link #onUpdateView}) - executed on the main/UI thread</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * client.addEventListener(new SdkEventListener() {
 *     @Override
 *     public void onReady(SplitClient client, SdkReadyMetadata metadata) {
 *         Boolean initialCacheLoad = metadata.isInitialCacheLoad();
 *         // Handle ready on background thread
 *     }
 *
 *     @Override
 *     public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
 *         SdkUpdateMetadata.Type type = metadata.getType(); // FLAGS_UPDATE or SEGMENTS_UPDATE
 *         List<String> names = metadata.getNames(); // updated flag/segment names
 *         // Handle updates on background thread
 *     }
 *
 *     @Override
 *     public void onReadyFromCacheView(SplitClient client, SdkReadyMetadata metadata) {
 *         // Handle cache ready on main/UI thread
 *         Boolean initialCacheLoad = metadata.isInitialCacheLoad();
 *     }
 * });
 * }</pre>
 */
public abstract class SdkEventListener {

    /**
     * Called when SDK_READY event occurs, executed on a background thread.
     * <p>
     * Override this method to handle SDK_READY events with typed metadata.
     *
     * @param client   the Split client instance
     * @param metadata the typed metadata containing ready state information
     */
    public void onReady(SplitClient client, SdkReadyMetadata metadata) {
        // Default empty implementation
    }

    /**
     * Called when SDK_READY event occurs, executed on the main/UI thread.
     * <p>
     * Override this method to handle SDK_READY events with typed metadata on the main thread.
     * Use this when you need to update UI components.
     *
     * @param client   the Split client instance
     * @param metadata the typed metadata containing ready state information
     */
    public void onReadyView(SplitClient client, SdkReadyMetadata metadata) {
        // Default empty implementation
    }

    /**
     * Called when SDK_UPDATE event occurs, executed on a background thread.
     * <p>
     * Override this method to handle SDK_UPDATE events with typed metadata.
     *
     * @param client   the Split client instance
     * @param metadata the typed metadata containing updated flag information
     */
    public void onUpdate(SplitClient client, SdkUpdateMetadata metadata) {
        // Default empty implementation
    }

    /**
     * Called when SDK_READY_FROM_CACHE event occurs, executed on a background thread.
     * <p>
     * Override this method to handle SDK_READY_FROM_CACHE events with typed metadata.
     *
     * @param client   the Split client instance
     * @param metadata the typed metadata containing cache information
     */
    public void onReadyFromCache(SplitClient client, SdkReadyMetadata metadata) {
        // Default empty implementation
    }

    /**
     * Called when SDK_UPDATE event occurs, executed on the main/UI thread.
     * <p>
     * Override this method to handle SDK_UPDATE events with typed metadata on the main thread.
     * Use this when you need to update UI components.
     *
     * @param client   the Split client instance
     * @param metadata the typed metadata containing updated flag information
     */
    public void onUpdateView(SplitClient client, SdkUpdateMetadata metadata) {
        // Default empty implementation
    }

    /**
     * Called when SDK_READY_FROM_CACHE event occurs, executed on the main/UI thread.
     * <p>
     * Override this method to handle SDK_READY_FROM_CACHE events with typed metadata on the main thread.
     * Use this when you need to update UI components.
     *
     * @param client   the Split client instance
     * @param metadata the typed metadata containing cache information
     */
    public void onReadyFromCacheView(SplitClient client, SdkReadyMetadata metadata) {
        // Default empty implementation
    }
}
