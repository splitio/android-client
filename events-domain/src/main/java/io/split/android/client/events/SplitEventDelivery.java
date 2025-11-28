package io.split.android.client.events;

import androidx.annotation.VisibleForTesting;

import io.harness.events.EventDelivery;
import io.harness.events.EventHandler;
import io.harness.events.Logging;
import io.split.android.client.api.EventMetadata;

/**
 * Event delivery implementation for Split SDK events.
 * <p>
 * Execution context (background vs main thread) should be
 * handled using {@link DualExecutorRegistration}.
 */
class SplitEventDelivery implements EventDelivery<SplitEvent, EventMetadata> {

    private final Logging mLogging;

    /**
     * Creates a new SplitEventDelivery with the default logging implementation.
     */
    public SplitEventDelivery() {
        this(new SplitLogging());
    }

    /**
     * Creates a new SplitEventDelivery with a custom logging implementation.
     *
     * @param logging the logging implementation to use
     */
    @VisibleForTesting
    SplitEventDelivery(Logging logging) {
        mLogging = logging != null ? logging : new SplitLogging();
    }

    @Override
    public void deliver(EventHandler<SplitEvent, EventMetadata> eventHandler,
                        SplitEvent event,
                        EventMetadata metadata) {
        if (eventHandler == null || event == null) {
            return;
        }

        try {
            eventHandler.handle(event, metadata);
        } catch (Exception e) {
            mLogging.logError("Exception delivering event " + event.name() + ": " + e.getMessage());
        }
    }
}
