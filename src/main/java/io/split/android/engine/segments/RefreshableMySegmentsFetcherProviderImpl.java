package io.split.android.engine.segments;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.utils.Logger;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutor;
import io.split.android.engine.scheduler.PausableScheduledThreadPoolExecutorImpl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A SegmentFetchers implementation that creates RefreshableSegmentFetcher instances.
 *
 */
public class RefreshableMySegmentsFetcherProviderImpl implements RefreshableMySegmentsFetcherProvider {

    private final Object _lock = new Object();
    private final PausableScheduledThreadPoolExecutor _scheduledExecutorService;
    private RefreshableMySegments _mySegments;


    public RefreshableMySegmentsFetcherProviderImpl(MySegmentsFetcher mySegmentsFetcher, long refreshEveryNSeconds, String matchingKey, ISplitEventsManager eventsManager) {

        checkNotNull(mySegmentsFetcher);
        checkNotNull(matchingKey);

        checkNotNull(eventsManager);

        checkArgument(refreshEveryNSeconds >= 0L);
        Logger.d("RefreshableMySegmentsFetcherProviderImpl: refreshEveryNSeconds %d", refreshEveryNSeconds);

        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setDaemon(true);
        threadFactoryBuilder.setNameFormat("split-mySegmentsFetcher-" + "%d");
        _scheduledExecutorService = PausableScheduledThreadPoolExecutorImpl.newSingleThreadScheduledExecutor(threadFactoryBuilder.build());

        _mySegments = RefreshableMySegments.create(matchingKey, mySegmentsFetcher, eventsManager);

        _scheduledExecutorService.scheduleWithFixedDelay(_mySegments, 0L, refreshEveryNSeconds, TimeUnit.SECONDS);

    }

    @Override
    public MySegments mySegments() {
        synchronized (_lock) {
            if (_mySegments != null) {
                return _mySegments;
            }

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

    @Override
    public void pause() {
        if (_scheduledExecutorService != null) {
            _scheduledExecutorService.pause();
        }
    }
    @Override
    public void resume() {
        if (_scheduledExecutorService != null) {
            _scheduledExecutorService.resume();
        }
    }
}
