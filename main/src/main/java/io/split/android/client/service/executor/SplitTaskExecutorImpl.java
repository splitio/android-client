package io.split.android.client.service.executor;

import androidx.annotation.NonNull;

import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutorImpl;

public class SplitTaskExecutorImpl extends SplitBaseTaskExecutor {

    private static final int MIN_THREAD_POOL_SIZE_WHEN_IDLE = 6;
    private static final String THREAD_NAME_FORMAT = "split-taskExecutor-%d";

    @NonNull
    @Override
    protected PausableScheduledThreadPoolExecutorImpl buildScheduler() {
        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setDaemon(true);
        threadFactoryBuilder.setNameFormat(THREAD_NAME_FORMAT);

        return new PausableScheduledThreadPoolExecutorImpl(MIN_THREAD_POOL_SIZE_WHEN_IDLE, threadFactoryBuilder.build());
    }
}
