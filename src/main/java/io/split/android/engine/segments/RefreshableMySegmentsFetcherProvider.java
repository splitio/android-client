package io.split.android.engine.segments;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.utils.Logger;
import io.split.android.engine.SDKReadinessGates;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A SegmentFetchers implementation that creates RefreshableSegmentFetcher instances.
 *
 */
public class RefreshableMySegmentsFetcherProvider implements Closeable {

    private final MySegmentsFetcher _mySegmentsFetcher;
    private final AtomicLong _refreshEveryNSeconds;

    private final Object _lock = new Object();
    private final ScheduledExecutorService _scheduledExecutorService;
    private RefreshableMySegments _mySegments;
    private String _matchingKey;
    private final SplitEventsManager _eventsManager;


    public RefreshableMySegmentsFetcherProvider(MySegmentsFetcher mySegmentsFetcher, long refreshEveryNSeconds, String matchingKey, SplitEventsManager eventsManager) {
        _mySegmentsFetcher = mySegmentsFetcher;
        checkNotNull(_mySegmentsFetcher);

        _matchingKey = matchingKey;
        checkNotNull(_matchingKey);

        _eventsManager = eventsManager;
        checkNotNull(_eventsManager);

        checkArgument(refreshEveryNSeconds >= 0L);
        _refreshEveryNSeconds = new AtomicLong(refreshEveryNSeconds);
        Logger.d("RefreshableMySegmentsFetcherProvider: refreshEveryNSeconds %d", refreshEveryNSeconds);

        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setDaemon(true);
        threadFactoryBuilder.setNameFormat("split-mySegmentsFetcher-" + "%d");
        _scheduledExecutorService = Executors.newScheduledThreadPool(1, threadFactoryBuilder.build());

    }

    public RefreshableMySegments mySegments() {
        synchronized (_lock) {
            if (_mySegments != null) {
                return _mySegments;
            }

            _mySegments = RefreshableMySegments.create(_matchingKey, _mySegmentsFetcher, _eventsManager);

            _scheduledExecutorService.scheduleWithFixedDelay(_mySegments, 0L, _refreshEveryNSeconds.get(), TimeUnit.SECONDS);

            return _mySegments;
        }
    }

    @Override
    public void close() {
        if (_scheduledExecutorService == null || _scheduledExecutorService.isShutdown()) {
            return;
        }
        _scheduledExecutorService.shutdown();
        try {
            if (!_scheduledExecutorService.awaitTermination(2L, TimeUnit.SECONDS)) { //optional *
                Logger.w("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = _scheduledExecutorService.shutdownNow(); //optional **
                Logger.w("Executor was abruptly shut down. These tasks will not be executed: %s", droppedTasks);
            }
        } catch (InterruptedException e) {
            // reset the interrupt.
            Logger.e(e,"Shutdown of SegmentFetchers was interrupted");
            Thread.currentThread().interrupt();
        }

    }
}
