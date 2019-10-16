package io.split.android.engine.experiments;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.Status;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.utils.Logger;


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An ExperimentFetcher that refreshes experiment definitions periodically.
 */
public class RefreshableSplitFetcher implements SplitFetcher, Runnable {

    private final SplitParser _parser;
    private final SplitChangeFetcher _splitChangeFetcher;
    private final AtomicLong _changeNumber;

    private Map<String, ParsedSplit> _concurrentMap = Maps.newConcurrentMap();
    private final ISplitEventsManager _eventsManager;

    private boolean _firstLoad = true;
    private boolean _shouldInitialize = true;

    private final Object _lock = new Object();

    public RefreshableSplitFetcher(SplitChangeFetcher splitChangeFetcher, SplitParser parser, ISplitEventsManager eventsManager) {
        this(splitChangeFetcher, parser, eventsManager, -1);
    }

    /**
     * This constructor is package private because it is meant primarily for unit tests
     * where we want to set the starting change number. All regular clients should use
     * the public constructor.
     *
     * @param splitChangeFetcher   MUST NOT be null
     * @param parser               MUST NOT be null
     * @param startingChangeNumber
     */
    /*package private*/ RefreshableSplitFetcher(SplitChangeFetcher splitChangeFetcher,
                                                SplitParser parser,
                                                ISplitEventsManager eventsManager,
                                                long startingChangeNumber) {
        _splitChangeFetcher = splitChangeFetcher;
        _parser = parser;
        _eventsManager = eventsManager;
        _changeNumber = new AtomicLong(startingChangeNumber);

        checkNotNull(_parser);
        checkNotNull(_splitChangeFetcher);

    }

    private void initializeFromCache() throws InterruptedException {
        SplitChange change = _splitChangeFetcher.fetch(-1, FetcherPolicy.CacheOnly);
        if (change != null && change.splits != null && !change.splits.isEmpty()) {
            synchronized (_lock) {
                parseChange(change);
            }
        }
    }

    @Override
    public void forceRefresh() {
        run();
    }

    public long changeNumber() {
        return _changeNumber.get();
    }


    @Override
    public ParsedSplit fetch(String test) {
        return _concurrentMap.get(test);
    }

    public List<ParsedSplit> fetchAll() {
        return Lists.newArrayList(_concurrentMap.values());
    }

    public Collection<ParsedSplit> fetch() {
        return _concurrentMap.values();
    }

    public void clear() {
        _concurrentMap.clear();
    }

    @Override
    public void run() {
        long start = _changeNumber.get();
        try {
            if (_shouldInitialize) {
                initializeFromCache();
                _shouldInitialize = false;
                if (!_splitChangeFetcher.isSourceReachable() && !_concurrentMap.isEmpty()) {
                    _eventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_ARE_READY);
                    _firstLoad = false;
                }
            }
            runWithoutExceptionHandling();
            if (_firstLoad) {
                _eventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_ARE_READY);
                _firstLoad = false;
            } else {
                _eventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_ARE_UPDATED);
            }

        } catch (InterruptedException e) {
            Logger.w(e, "Interrupting split fetcher task");
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            Logger.e(t, "RefreshableSplitFetcher failed: %s", t.getMessage());
        } finally {
            try {
                Logger.d("split fetch before: %d, after: %d", start, _changeNumber.get());
            } catch (Exception e) {
                Logger.e(e);
            }
        }
    }

    public void runWithoutExceptionHandling() throws InterruptedException {
        SplitChange change = _splitChangeFetcher.fetch(_changeNumber.get());

        if (change == null) {
            throw new IllegalStateException("SplitChange was null");
        }

        if (change.till == _changeNumber.get()) {
            // no change.
            return;
        }

        if (change.since != _changeNumber.get()
                || change.till < _changeNumber.get()) {
            // some other thread may have updated the shared state. exit
            return;
        }

        if (change.splits.isEmpty()) {
            // there are no changes. weird!
            _changeNumber.set(change.till);
            return;
        }

        synchronized (_lock) {
            parseChange(change);
            _changeNumber.set(change.till);
        }
    }

    public void parseChange(SplitChange change) throws InterruptedException {


        Set<String> toRemove = Sets.newHashSet();
        Map<String, ParsedSplit> toAdd = Maps.newHashMap();

        for (Split split : change.splits) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            if (split.status != Status.ACTIVE) {
                // archive.
                toRemove.add(split.name);
                continue;
            }

            ParsedSplit parsedSplit = _parser.parse(split);
            if (parsedSplit == null) {
                Logger.i("We could not parse the experiment definition for: %s so we are removing it completely to be careful", split.name);
                toRemove.add(split.name);
                continue;
            }

            toAdd.put(split.name, parsedSplit);
        }

        _concurrentMap.putAll(toAdd);
        for (String remove : toRemove) {
            _concurrentMap.remove(remove);
        }

        if (!toAdd.isEmpty()) {
            Logger.d("Updated features: %s", toAdd.keySet());
        }

        if (!toRemove.isEmpty()) {
            Logger.d("Deleted features: %s", toRemove);
        }
    }
}
