package io.split.android.client.backend;

public interface SplitTaskExecutor {
    void schedule(SplitTask task, long initialDelayInSecs, long periodInSecs);
    void submit(SplitTask task);
    void pause();
    void resume();
    void stop();
}
