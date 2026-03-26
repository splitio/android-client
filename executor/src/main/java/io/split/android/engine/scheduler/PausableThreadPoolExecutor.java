package io.split.android.engine.scheduler;

import java.util.concurrent.ExecutorService;

public interface PausableThreadPoolExecutor extends ExecutorService {
    void pause();
    void resume();
}
