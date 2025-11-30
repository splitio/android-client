package io.split.android.client.events;

import static java.util.Objects.requireNonNull;

import androidx.annotation.VisibleForTesting;

import java.util.concurrent.Executor;

import io.harness.events.EventHandler;
import io.harness.events.EventsManager;
import io.harness.events.EventsManagers;
import io.split.android.client.api.EventMetadata;
import io.split.android.client.events.executors.SplitEventExecutorResources;
import io.split.android.client.events.executors.SplitEventExecutorResourcesImpl;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.utils.logger.Logger;

/**
 * Events manager for Split SDK.
 */
public class SplitEventsManager implements ISplitEventsManager, ListenableEventsManager {

    private final EventsManager<SplitEvent, SplitInternalEvent, EventMetadata> mEventsManager;
    private final DualExecutorRegistration<SplitEvent, SplitInternalEvent, EventMetadata> mDualExecutorRegistration;
    private SplitEventExecutorResources mResources;

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
        mEventsManager.notifyInternalEvent(internalEvent, null);
    }

    /**
     * Notifies an internal event with metadata.
     *
     * @param internalEvent the internal event
     * @param metadata      the event metadata
     */
    public void notifyInternalEvent(SplitInternalEvent internalEvent, EventMetadata metadata) {
        requireNonNull(internalEvent);
        mEventsManager.notifyInternalEvent(internalEvent, metadata);
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
        return command -> taskExecutor.submit(() -> {
            try {
                command.run();
            } catch (Exception e) {
                Logger.e("Error in background executor: " + e.getMessage());
            }
            return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        }, null);
    }

    private Executor createMainThreadExecutor(final SplitTaskExecutor taskExecutor) {
        return command -> taskExecutor.submitOnMainThread(() -> {
            try {
                command.run();
            } catch (Exception e) {
                Logger.e("Error in main thread executor: " + e.getMessage());
            }
            return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        });
    }
}
