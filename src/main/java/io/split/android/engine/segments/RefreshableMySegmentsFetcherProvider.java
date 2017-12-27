package io.split.android.engine.segments;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.engine.SDKReadinessGates;
import timber.log.Timber;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A SegmentFetchers implementation that creates RefreshableSegmentFetcher instances.
 *
 * @author adil
 */
public class RefreshableMySegmentsFetcherProvider implements Closeable {

    private final MySegmentsFetcher _mySegmentsFetcher;
    private final AtomicLong _refreshEveryNSeconds;

    private final Object _lock = new Object();
    private final ScheduledExecutorService _scheduledExecutorService;
    private final SDKReadinessGates _gates;
    private RefreshableMySegments _mySegments;
    private String _matchingKey;


    public RefreshableMySegmentsFetcherProvider(MySegmentsFetcher mySegmentsFetcher, long refreshEveryNSeconds, String matchingKey, SDKReadinessGates sdkBuildBlocker) {
        _mySegmentsFetcher = mySegmentsFetcher;
        checkNotNull(_mySegmentsFetcher);

        _matchingKey = matchingKey;
        checkNotNull(_matchingKey);

        _gates = sdkBuildBlocker;
        checkNotNull(_gates);

        checkArgument(refreshEveryNSeconds >= 0L);
        _refreshEveryNSeconds = new AtomicLong(refreshEveryNSeconds);

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

            _mySegments = RefreshableMySegments.create(_matchingKey, _mySegmentsFetcher, _gates);

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
                Timber.i("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = _scheduledExecutorService.shutdownNow(); //optional **
                Timber.i("Executor was abruptly shut down. These tasks will not be executed: %s", droppedTasks);
            }
        } catch (InterruptedException e) {
            // reset the interrupt.
            Timber.e("Shutdown of SegmentFetchers was interrupted");
            Thread.currentThread().interrupt();
        }

    }
}
