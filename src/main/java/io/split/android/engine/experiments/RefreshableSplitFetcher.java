package io.split.android.engine.experiments;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.Matcher;
import io.split.android.client.dtos.MatcherType;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.Status;
import io.split.android.engine.SDKReadinessGates;
import timber.log.Timber;


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An ExperimentFetcher that refreshes experiment definitions periodically.
 *
 * @author adil
 */
public class RefreshableSplitFetcher implements SplitFetcher, Runnable {

    private final SplitParser _parser;
    private final SplitChangeFetcher _splitChangeFetcher;
    private final AtomicLong _changeNumber;

    private Map<String, ParsedSplit> _concurrentMap = Maps.newConcurrentMap();
    private final SDKReadinessGates _gates;

    private final Object _lock = new Object();


    public RefreshableSplitFetcher(SplitChangeFetcher splitChangeFetcher, SplitParser parser, SDKReadinessGates gates) {
        this(splitChangeFetcher, parser, gates, -1);
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
                                                SDKReadinessGates gates,
                                                long startingChangeNumber) {
        _splitChangeFetcher = splitChangeFetcher;
        _parser = parser;
        _gates = gates;
        _changeNumber = new AtomicLong(startingChangeNumber);

        checkNotNull(_parser);
        checkNotNull(_splitChangeFetcher);
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
            runWithoutExceptionHandling();
            _gates.splitsAreReady();
        } catch (InterruptedException e) {
            Timber.w("Interrupting split fetcher task");
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            Timber.e("RefreshableSplitFetcher failed: %s" , t.getMessage());
            Timber.d(t);
        } finally {
            try {
                Timber.d("split fetch before: %d, after: %d", start, _changeNumber.get());
            } catch (Exception e) {
                Timber.e(e);
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

        Set<String> segmentsInUse = Sets.newHashSet();


        synchronized (_lock) {
            // check state one more time.
            if (change.since != _changeNumber.get()
                    || change.till < _changeNumber.get()) {
                // some other thread may have updated the shared state. exit
                return;
            }

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
                    Timber.i("We could not parse the experiment definition for: %s so we are removing it completely to be careful", split.name);
                    toRemove.add(split.name);
                    continue;
                }

                segmentsInUse.addAll(collectSegmentsInUse(split));
                toAdd.put(split.name, parsedSplit);
            }

            _concurrentMap.putAll(toAdd);
            for (String remove : toRemove) {
                _concurrentMap.remove(remove);
            }

            if (!toAdd.isEmpty()) {
                Timber.d("Updated features: %s", toAdd.keySet());
            }

            if (!toRemove.isEmpty()) {
                Timber.d("Deleted features: %s", toRemove);
            }

            _changeNumber.set(change.till);
        }

        _gates.registerSegments(segmentsInUse);
    }

    private List<String> collectSegmentsInUse(Split split) {
        List<String> result = Lists.newArrayList();
        for (Condition condition : split.conditions) {
            for (Matcher matcher : condition.matcherGroup.matchers) {
                if (matcher.matcherType == MatcherType.IN_SEGMENT) {
                    if (matcher.userDefinedSegmentMatcherData != null && matcher.userDefinedSegmentMatcherData.segmentName != null) {
                        result.add(matcher.userDefinedSegmentMatcherData.segmentName);
                    }
                }
            }
        }
        return result;
    }
}
