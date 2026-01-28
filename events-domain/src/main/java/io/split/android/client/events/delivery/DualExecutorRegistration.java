package io.split.android.client.events.delivery;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

import io.harness.events.EventHandler;
import io.harness.events.EventsManager;
import io.harness.events.Logging;
import io.split.android.client.events.logging.SplitLogging;

/**
 * Utility for registering event handlers that need to execute on two different threads.
 * <p>
 * This is useful when an event should trigger both background work and UI updates.
 * Each callback is wrapped with its executor before registration.
 *
 * @param <E> event type
 * @param <I> internal event type (for EventsManager)
 * @param <M> metadata type
 */
public class DualExecutorRegistration<E, I, M> {

    @NonNull
    private final Executor mBackgroundExecutor;
    @NonNull
    private final Executor mMainThreadExecutor;
    @NonNull
    private final Logging mLogging;

    /**
     * Creates a new DualExecutorRegistration with a {@link SplitLogging} instance.
     *
     * @param backgroundExecutor executor for background execution
     * @param mainThreadExecutor executor for main thread execution
     */
    public DualExecutorRegistration(@NonNull Executor backgroundExecutor,
                                    @NonNull Executor mainThreadExecutor) {
        this(backgroundExecutor, mainThreadExecutor, new SplitLogging());
    }

    /**
     * Creates a new DualExecutorRegistration.
     * <p>
     * Package-private for testing.
     *
     * @param backgroundExecutor executor for background execution
     * @param mainThreadExecutor executor for main thread execution
     * @param logging            logging instance
     */
    DualExecutorRegistration(@NonNull Executor backgroundExecutor,
                             @NonNull Executor mainThreadExecutor,
                             @NonNull Logging logging) {
        if (backgroundExecutor == null) {
            throw new IllegalArgumentException("backgroundExecutor cannot be null");
        }
        if (mainThreadExecutor == null) {
            throw new IllegalArgumentException("mainThreadExecutor cannot be null");
        }
        if (logging == null) {
            throw new IllegalArgumentException("logging cannot be null");
        }
        mBackgroundExecutor = backgroundExecutor;
        mMainThreadExecutor = mainThreadExecutor;
        mLogging = logging;
    }

    /**
     * Registers two handlers for the same event, each executing on its respective thread.
     *
     * @param eventsManager      the events manager to register with
     * @param event              the event to register for
     * @param backgroundCallback callback to execute on the background thread
     * @param mainThreadCallback callback to execute on the main thread
     */
    public void register(@NonNull EventsManager<E, I, M> eventsManager,
                         @NonNull E event,
                         @NonNull EventHandler<E, M> backgroundCallback,
                         @NonNull EventHandler<E, M> mainThreadCallback) {
        if (eventsManager == null || event == null) {
            return;
        }

        if (backgroundCallback != null) {
            eventsManager.register(event, wrapWithExecutor(backgroundCallback, mBackgroundExecutor));
        }

        if (mainThreadCallback != null) {
            eventsManager.register(event, wrapWithExecutor(mainThreadCallback, mMainThreadExecutor));
        }
    }

    /**
     * Registers a single handler for the background thread only.
     *
     * @param eventsManager      the events manager to register with
     * @param event              the event to register for
     * @param backgroundCallback callback to execute on the background thread
     */
    public void registerBackground(@NonNull EventsManager<E, I, M> eventsManager,
                                   @NonNull E event,
                                   @NonNull EventHandler<E, M> backgroundCallback) {
        if (eventsManager == null || event == null || backgroundCallback == null) {
            return;
        }
        eventsManager.register(event, wrapWithExecutor(backgroundCallback, mBackgroundExecutor));
    }

    /**
     * Registers a single handler for the main thread only.
     *
     * @param eventsManager      the events manager to register with
     * @param event              the event to register for
     * @param mainThreadCallback callback to execute on the main thread
     */
    public void registerMainThread(@NonNull EventsManager<E, I, M> eventsManager,
                                   @NonNull E event,
                                   @NonNull EventHandler<E, M> mainThreadCallback) {
        if (eventsManager == null || event == null || mainThreadCallback == null) {
            return;
        }
        eventsManager.register(event, wrapWithExecutor(mainThreadCallback, mMainThreadExecutor));
    }

    private EventHandler<E, M> wrapWithExecutor(EventHandler<E, M> handler, Executor executor) {
        return (event, metadata) -> executor.execute(() -> {
            try {
                handler.handle(event, metadata);
            } catch (Exception e) {
                mLogging.logError("Exception in event handler: " + e.getMessage());
            }
        });
    }
}
