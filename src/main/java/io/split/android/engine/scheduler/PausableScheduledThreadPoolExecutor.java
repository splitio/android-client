package io.split.android.engine.scheduler;

import java.util.concurrent.ScheduledExecutorService;

public interface PausableScheduledThreadPoolExecutor extends ScheduledExecutorService {
    void pause();
    void resume();
}
