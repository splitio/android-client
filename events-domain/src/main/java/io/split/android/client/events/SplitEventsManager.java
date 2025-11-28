package io.split.android.client.events;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.Executor;

import io.harness.events.EventHandler;
import io.harness.events.EventsManager;
import io.harness.events.EventsManagers;
import io.split.android.client.SplitClient;
import io.split.android.client.api.EventMetadata;
import io.split.android.client.events.executors.SplitEventExecutorResources;
import io.split.android.client.events.executors.SplitEventExecutorResourcesImpl;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.utils.logger.Logger;

/**
 * Events manager for Split SDK.
 */
public class SplitEventsManager implements ISplitEventsManager, ListenableEventsManager {

    private final EventsManager<SplitEvent, SplitInternalEvent, EventMetadata> mEventsManager;
    private final DualExecutorRegistration<SplitEvent, SplitInternalEvent, EventMetadata> mDualExecutorRegistration;
    private SplitEventExecutorResources mResources;

    // Track sync completion for SDK_READY_FROM_CACHE triggering. TODO: This is a temporary adaptation before extending EventsManager requireAny.
    private volatile boolean mSplitsSyncComplete = false;
    private volatile boolean mSegmentsSyncComplete = false;

    /**
     * Creates a new SplitEventsManager.
     *
     * @param splitTaskExecutor the task executor for running callbacks
     * @param blockUntilReady   timeout in milliseconds for SDK_READY (0 = no timeout)
     */
    public SplitEventsManager(SplitTaskExecutor splitTaskExecutor, final int blockUntilReady) {
        requireNonNull(splitTaskExecutor);

        mResources = new SplitEventExecutorResourcesImpl();

        // Create the events manager with Split SDK configuration
        mEventsManager = EventsManagers.create(
                SplitEventsManagerConfigFactory.create(),
                new SplitEventDelivery()
        );

        // Create the dual executor registration for handling background + main thread callbacks
        mDualExecutorRegistration = new DualExecutorRegistration<>(
                createBackgroundExecutor(splitTaskExecutor),
                createMainThreadExecutor(splitTaskExecutor)
        );

        // Start timeout thread if configured
        if (blockUntilReady > 0) {
            startTimeoutThread(blockUntilReady);
        }
    }

    /**
     * Package-private constructor for testing.
     */
    @VisibleForTesting
    SplitEventsManager(EventsManager<SplitEvent, SplitInternalEvent, EventMetadata> eventsManager,
                       DualExecutorRegistration<SplitEvent, SplitInternalEvent, EventMetadata> dualExecutorRegistration,
                       SplitEventExecutorResources resources) {
        mEventsManager = eventsManager;
        mDualExecutorRegistration = dualExecutorRegistration;
        mResources = resources;
    }

    @VisibleForTesting
    public void setExecutionResources(SplitEventExecutorResources resources) {
        mResources = resources;
    }

    @Override
    public SplitEventExecutorResources getExecutorResources() {
        return mResources;
    }

    @Override
    public void notifyInternalEvent(SplitInternalEvent internalEvent) {
        requireNonNull(internalEvent);

        // Skip FETCHED events after SDK_READY to prevent unnecessary SDK_UPDATE triggers.
        // TODO: This is temporary until *_FETCHED and *_UPDATED events are unified.
        if ((internalEvent == SplitInternalEvent.SPLITS_FETCHED
                || internalEvent == SplitInternalEvent.MY_SEGMENTS_FETCHED)
                && eventAlreadyTriggered(SplitEvent.SDK_READY)) {
            return;
        }

        // Notify the actual internal event
        mEventsManager.notifyInternalEvent(internalEvent, null);

        // Also notify the synthetic composite events for SDK_READY evaluation
        notifySyntheticEventsIfNeeded(internalEvent);
    }

    /**
     * Notifies an internal event with metadata.
     *
     * @param internalEvent the internal event
     * @param metadata      the event metadata
     */
    public void notifyInternalEvent(SplitInternalEvent internalEvent, EventMetadata metadata) {
        requireNonNull(internalEvent);

        // Skip FETCHED events after SDK_READY
        if ((internalEvent == SplitInternalEvent.SPLITS_FETCHED
                || internalEvent == SplitInternalEvent.MY_SEGMENTS_FETCHED)
                && eventAlreadyTriggered(SplitEvent.SDK_READY)) {
            return;
        }

        mEventsManager.notifyInternalEvent(internalEvent, metadata);
        notifySyntheticEventsIfNeeded(internalEvent);
    }

    @Override
    public void register(SplitEvent event, SplitEventTask task) {
        requireNonNull(event);
        requireNonNull(task);

        // Adapt SplitEventTask to EventHandler and register for both threads
        mDualExecutorRegistration.register(
                mEventsManager,
                event,
                createBackgroundHandler(task),
                createMainThreadHandler(task)
        );
    }

    @Override
    public boolean eventAlreadyTriggered(SplitEvent event) {
        return mEventsManager.eventAlreadyTriggered(event);
    }

    /**
     * Destroys this events manager.
     * After calling this method, the manager will no longer process events.
     */
    public void destroy() {
        mEventsManager.destroy();
    }

    /**
     * Notifies the synthetic composite events based on the actual internal event.
     * These synthetic events simplify the SDK_READY condition evaluation.
     * <p>
     * Also handles the special case where SDK_READY_FROM_CACHE should fire
     * before SDK_READY when both sync events have completed.
     */
    private void notifySyntheticEventsIfNeeded(SplitInternalEvent internalEvent) {
        switch (internalEvent) {
            case SPLITS_UPDATED:
            case SPLITS_FETCHED:
                mSplitsSyncComplete = true;
                // Check if SDK_READY_FROM_CACHE should fire BEFORE notifying the sync complete
                // to ensure correct ordering (SDK_READY_FROM_CACHE before SDK_READY)
                triggerReadyFromCacheIfNeeded();
                mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_SYNC_COMPLETE, null);
                break;

            case MY_SEGMENTS_UPDATED:
            case MY_SEGMENTS_FETCHED:
            case MY_LARGE_SEGMENTS_UPDATED:
                mSegmentsSyncComplete = true;
                // Check if SDK_READY_FROM_CACHE should fire BEFORE notifying the sync complete
                triggerReadyFromCacheIfNeeded();
                mEventsManager.notifyInternalEvent(SplitInternalEvent.SEGMENTS_SYNC_COMPLETE, null);
                break;

            default:
                // No synthetic event needed for other internal events
                break;
        }
    }

    /**
     * Triggers SDK_READY_FROM_CACHE if SDK_READY is about to fire (both sync events received)
     * but SDK_READY_FROM_CACHE hasn't fired yet.
     * <p>
     * TODO: This is a temporary adaptation before extending EventsManager requireAny.
     */
    private void triggerReadyFromCacheIfNeeded() {
        // Only trigger if both sync events have completed
        if (!mSplitsSyncComplete || !mSegmentsSyncComplete) {
            return;
        }

        // If SDK_READY_FROM_CACHE already triggered, nothing to do
        if (mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY_FROM_CACHE)) {
            return;
        }

        // If SDK_READY already triggered, nothing to do (too late)
        if (mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)) {
            return;
        }

        // Both sync events received and SDK_READY_FROM_CACHE not yet fired
        // Trigger it by firing all required cache events
        mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE, null);
        mEventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE, null);
        mEventsManager.notifyInternalEvent(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE, null);
        mEventsManager.notifyInternalEvent(SplitInternalEvent.ENCRYPTION_MIGRATION_DONE, null);
    }

    private void startTimeoutThread(final int blockUntilReady) {
        Thread timeoutThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(blockUntilReady);
                    mEventsManager.notifyInternalEvent(SplitInternalEvent.SDK_READY_TIMEOUT_REACHED, null);
                } catch (InterruptedException e) {
                    Logger.d("Waiting before to check if SDK is READY has been interrupted", e.getMessage());
                    mEventsManager.notifyInternalEvent(SplitInternalEvent.SDK_READY_TIMEOUT_REACHED, null);
                } catch (Throwable e) {
                    Logger.d("Waiting before to check if SDK is READY interrupted ", e.getMessage());
                    mEventsManager.notifyInternalEvent(SplitInternalEvent.SDK_READY_TIMEOUT_REACHED, null);
                }
            }
        });
        timeoutThread.setName("Split-SDKReadyTimeout");
        timeoutThread.setDaemon(true);
        timeoutThread.start();
    }

    private EventHandler<SplitEvent, EventMetadata> createBackgroundHandler(final SplitEventTask task) {
        return new EventHandler<SplitEvent, EventMetadata>() {
            @Override
            public void handle(SplitEvent event, EventMetadata metadata) {
                try {
                    task.onPostExecution(mResources.getSplitClient());
                } catch (SplitEventTaskMethodNotImplementedException e) {
                    // Method not implemented by client, ignore
                } catch (Exception e) {
                    Logger.e("Error executing background event task: " + e.getMessage());
                }
            }
        };
    }

    private EventHandler<SplitEvent, EventMetadata> createMainThreadHandler(final SplitEventTask task) {
        return new EventHandler<SplitEvent, EventMetadata>() {
            @Override
            public void handle(SplitEvent event, EventMetadata metadata) {
                try {
                    task.onPostExecutionView(mResources.getSplitClient());
                } catch (SplitEventTaskMethodNotImplementedException e) {
                    // Method not implemented by client, ignore
                } catch (Exception e) {
                    Logger.e("Error executing main thread event task: " + e.getMessage());
                }
            }
        };
    }

    private Executor createBackgroundExecutor(final SplitTaskExecutor taskExecutor) {
        return new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                taskExecutor.submit(new io.split.android.client.service.executor.SplitTask() {
                    @NonNull
                    @Override
                    public io.split.android.client.service.executor.SplitTaskExecutionInfo execute() {
                        try {
                            command.run();
                        } catch (Exception e) {
                            Logger.e("Error in background executor: " + e.getMessage());
                        }
                        return io.split.android.client.service.executor.SplitTaskExecutionInfo.success(
                                io.split.android.client.service.executor.SplitTaskType.GENERIC_TASK);
                    }
                }, null);
            }
        };
    }

    private Executor createMainThreadExecutor(final SplitTaskExecutor taskExecutor) {
        return new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                taskExecutor.submitOnMainThread(new io.split.android.client.service.executor.SplitTask() {
                    @NonNull
                    @Override
                    public io.split.android.client.service.executor.SplitTaskExecutionInfo execute() {
                        try {
                            command.run();
                        } catch (Exception e) {
                            Logger.e("Error in main thread executor: " + e.getMessage());
                        }
                        return io.split.android.client.service.executor.SplitTaskExecutionInfo.success(
                                io.split.android.client.service.executor.SplitTaskType.GENERIC_TASK);
                    }
                });
            }
        };
    }
}
