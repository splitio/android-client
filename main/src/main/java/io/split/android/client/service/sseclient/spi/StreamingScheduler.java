package io.split.android.client.service.sseclient.spi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface for scheduling delayed tasks within the streaming module.
 * Implementations should provide timer/scheduling capabilities backed
 * by the host application's task executor.
 */
public interface StreamingScheduler {

    /**
     * Schedules a task to run after the specified delay.
     *
     * @param task the runnable to execute
     * @param delaySeconds delay before execution in seconds
     * @param listener optional listener to be notified when task completes
     * @return a unique task ID that can be used to cancel the task
     */
    @NonNull
    String schedule(@NonNull Runnable task, long delaySeconds, @Nullable TaskExecutionListener listener);

    /**
     * Cancels a previously scheduled task.
     *
     * @param taskId the ID returned by schedule()
     */
    void cancel(@Nullable String taskId);

    /**
     * Listener interface for task completion notifications.
     */
    interface TaskExecutionListener {
        /**
         * Called when a scheduled task has completed execution.
         */
        void onTaskExecuted();
    }
}
