package io.split.android.client.service.executor;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutorImpl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class SplitTaskExecutorImpl extends SplitBaseTaskExecutor {

    private static final int MIN_THREADPOOL_SIZE_WHEN_IDLE = 6;
    private static final String THREAD_NAME_FORMAT = "split-taskExecutor-%d";

    @NonNull
    @Override
    protected PausableScheduledThreadPoolExecutorImpl buildScheduler() {
        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setDaemon(true);
        threadFactoryBuilder.setNameFormat(THREAD_NAME_FORMAT);
        return new PausableScheduledThreadPoolExecutorImpl(MIN_THREADPOOL_SIZE_WHEN_IDLE, threadFactoryBuilder.build());
    }
}
