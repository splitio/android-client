package io.split.android.client.events;

/**
 * Type-safe event class for SDK event subscriptions.
 * <p>
 * This class provides compile-time type safety for event task registration.
 * Use the static instances to register event listeners with the correct task type.
 * <p>
 * Example usage:
 * <pre>{@code
 * client.on(SdkEvent.SDK_UPDATE, new SdkUpdateEventTask() {
 *     @Override
 *     public void onPostExecution(SplitClient client, SdkUpdateMetadata metadata) {
 *         List<String> flags = metadata.getUpdatedFlags();
 *     }
 * });
 * }</pre>
 *
 * @param <T> the type of event task that can handle this event
 */
public abstract class SdkEvent<T extends SplitEventTask> {

    /**
     * Event fired when SDK definitions are updated from the server.
     * <p>
     * Register with {@link SdkUpdateEventTask} to receive typed metadata.
     */
    public static final SdkEvent<SdkUpdateEventTask> SDK_UPDATE = new SdkEvent<SdkUpdateEventTask>() {
        @Override
        public SplitEvent toSplitEvent() {
            return SplitEvent.SDK_UPDATE;
        }
    };

    /**
     * Event fired when SDK is ready from cached data.
     * <p>
     * Register with {@link SdkReadyFromCacheEventTask} to receive typed metadata.
     */
    public static final SdkEvent<SdkReadyFromCacheEventTask> SDK_READY_FROM_CACHE = new SdkEvent<SdkReadyFromCacheEventTask>() {
        @Override
        public SplitEvent toSplitEvent() {
            return SplitEvent.SDK_READY_FROM_CACHE;
        }
    };

    /**
     * Event fired when SDK is fully ready from the server.
     * <p>
     * Register with {@link SplitEventTask} for basic event handling.
     */
    public static final SdkEvent<SplitEventTask> SDK_READY = new SdkEvent<SplitEventTask>() {
        @Override
        public SplitEvent toSplitEvent() {
            return SplitEvent.SDK_READY;
        }
    };

    /**
     * Event fired when SDK ready has timed out.
     * <p>
     * Register with {@link SplitEventTask} for basic event handling.
     */
    public static final SdkEvent<SplitEventTask> SDK_READY_TIMED_OUT = new SdkEvent<SplitEventTask>() {
        @Override
        public SplitEvent toSplitEvent() {
            return SplitEvent.SDK_READY_TIMED_OUT;
        }
    };

    // Package-private constructor to prevent external subclassing
    SdkEvent() {
    }

    /**
     * Converts this type-safe event to the internal SplitEvent enum.
     * <p>
     * Internal API - called by SDK internals.
     *
     * @return the corresponding SplitEvent enum value
     */
    public abstract SplitEvent toSplitEvent();
}

