package io.split.android.client.service.executor;

import androidx.annotation.NonNull;

import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutor;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutorImpl;

public class SplitSingleThreadTaskExecutor extends SplitBaseTaskExecutor {

    private static final String THREAD_NAME_FORMAT = "split-singleThreadTaskExecutor-%d";

    @NonNull
    @Override
    protected PausableScheduledThreadPoolExecutor buildScheduler() {
        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setDaemon(true);
        threadFactoryBuilder.setNameFormat(THREAD_NAME_FORMAT);

        return PausableScheduledThreadPoolExecutorImpl.newSingleThreadScheduledExecutor(threadFactoryBuilder.build());
    }
}
