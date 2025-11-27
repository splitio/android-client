package io.harness.events;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;

/**
 * Event delivery implementation that executes handlers on a provided executor.
 * <p>
 * For synchronous delivery, use {@link Runnable#run()} as the executor.
 *
 * @param <E> event type
 * @param <M> metadata type
 */
public class ExecutorEventDelivery<E, M> implements EventDelivery<E, M> {

    @NotNull
    private final Executor mExecutor;
    @NotNull
    private final Logging mLogging;

    /**
     * Creates a new ExecutorEventDelivery with optional logging.
     *
     * @param executor the executor to use for delivering events. If null, a direct
     *                 executor (synchronous execution) will be used.
     * @param logging  optional logging implementation for diagnostic output
     */
    public ExecutorEventDelivery(@Nullable Executor executor, @Nullable Logging logging) {
        mExecutor = executor != null ? executor : Runnable::run;
        mLogging = logging != null ? logging : NoOpLogging.INSTANCE;
    }

    /**
     * Creates an ExecutorEventDelivery with synchronous execution.
     *
     * @param <E> event type
     * @param <M> metadata type
     * @return a new ExecutorEventDelivery that executes handlers synchronously
     */
    public static <E, M> ExecutorEventDelivery<E, M> synchronous() {
        return new ExecutorEventDelivery<>(null, null);
    }

    @Override
    public void deliver(EventHandler<E, M> eventHandler, E event, M metadata) {
        if (eventHandler == null) {
            return;
        }

        mExecutor.execute(() -> {
            try {
                eventHandler.handle(event, metadata);
            } catch (Exception e) {
                mLogging.logError("Exception in event handler for event " + event + ": " + e.getMessage());
            }
        });
    }
}
