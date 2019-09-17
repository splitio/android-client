package io.split.android.engine.experiments;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.utils.Logger;
import io.split.android.engine.SDKReadinessGates;


import java.io.Closeable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides an instance of RefreshableExperimentFetcher that is guaranteed to be a singleton.
 *
 */
public class RefreshableSplitFetcherProvider implements Closeable {

    private final SplitParser _splitParser;
    private final SplitChangeFetcher _splitChangeFetcher;
    private final AtomicLong _refreshEveryNSeconds;
    private final AtomicReference<RefreshableSplitFetcher> _splitFetcher = new AtomicReference<RefreshableSplitFetcher>();
    private final SplitEventsManager _eventsManager;
    private final AtomicReference<ScheduledExecutorService> _executorService = new AtomicReference<>();

    private final Object _lock = new Object();

    private final long _initialChangeNumber;


    public RefreshableSplitFetcherProvider(SplitChangeFetcher splitChangeFetcher, SplitParser splitParser, long refreshEveryNSeconds, SplitEventsManager eventsManager, long initialChangeNumber) {
        _splitChangeFetcher = splitChangeFetcher;
        checkNotNull(_splitChangeFetcher);

        _splitParser = splitParser;
        checkNotNull(_splitParser);

        checkArgument(refreshEveryNSeconds >= 0L);
        _refreshEveryNSeconds = new AtomicLong(refreshEveryNSeconds);

        _eventsManager = eventsManager;
        checkNotNull(_eventsManager);

        _initialChangeNumber = initialChangeNumber;

    }

    public RefreshableSplitFetcher getFetcher() {
        if (_splitFetcher.get() != null) {
            return _splitFetcher.get();
        }

        // we are locking here since we wanna make sure that we create only ONE RefreshableExperimentChangeFetcher
        synchronized (_lock) {
            // double check
            if (_splitFetcher.get() != null) {
                return _splitFetcher.get();
            }

            RefreshableSplitFetcher splitFetcher = new RefreshableSplitFetcher(_splitChangeFetcher, _splitParser, _eventsManager, _initialChangeNumber);

            ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
            threadFactoryBuilder.setDaemon(true);
            threadFactoryBuilder.setNameFormat("split-splitFetcher-%d");

            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactoryBuilder.build());
            scheduledExecutorService.scheduleWithFixedDelay(splitFetcher, 0L, _refreshEveryNSeconds.get(), TimeUnit.SECONDS);
            _executorService.set(scheduledExecutorService);

            _splitFetcher.set(splitFetcher);
            return splitFetcher;
        }
    }

    @Override
    public void close() {
        if (_executorService.get() == null) {
            return;
        }

        if (_splitFetcher.get() != null) {
            _splitFetcher.get().clear();
        }

        ScheduledExecutorService scheduledExecutorService = _executorService.get();
        if (scheduledExecutorService.isShutdown()) {
            return;
        }

        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(2L, TimeUnit.SECONDS)) { //optional *
                Logger.w("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = scheduledExecutorService.shutdownNow(); //optional **
                Logger.w("Executor was abruptly shut down. These tasks will not be executed: %s", droppedTasks);
            }
        } catch (InterruptedException e) {
            // reset the interrupt.
            Logger.w("Shutdown hook for split fetchers has been interrupted");
            Thread.currentThread().interrupt();
        }
    }

}
