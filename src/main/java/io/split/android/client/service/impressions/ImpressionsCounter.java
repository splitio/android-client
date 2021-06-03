package io.split.android.client.service.impressions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImpressionsCounter {

    public static class Key {
        private final String featureName;
        private final long timeFrame;

        public Key(String featureName, long timeframe) {
            this.featureName = checkNotNull(featureName);
            timeFrame = timeframe;
        }

        public String featureName() { return featureName; }
        public long timeFrame() { return timeFrame; }

        @Override
        public int hashCode() {
            return String.format("%s%d", featureName, timeFrame).hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;
            return featureName.equals(key.featureName) &&
                    timeFrame == key.timeFrame;
        }
    }

    private final ConcurrentHashMap<Key, AtomicInteger> mCounts;

    public ImpressionsCounter() {
        mCounts = new ConcurrentHashMap<>();
    }

    public void inc(String featureName, long timeFrame, int amount) {
        Key key = new Key(featureName, ImpressionUtils.truncateTimeframe(timeFrame));
        AtomicInteger count = mCounts.get(key);
        if (count == null) {
            count = new AtomicInteger();
            AtomicInteger old = mCounts.putIfAbsent(key, count);
            if (old != null) { // Some other thread won the race, use that AtomicInteger instead
                count = old;
            }
        }
        count.addAndGet(amount);
    }

    public List<ImpressionsCountPerFeature> popAll() {
        List<ImpressionsCountPerFeature> counts = new ArrayList<>();
        List<Key> keys = new ArrayList(mCounts.keySet());
        for (Key key : keys) {
            AtomicInteger currentCount = mCounts.remove(key);
            if(currentCount != null) {
                counts.add(new ImpressionsCountPerFeature(key.featureName, key.timeFrame, currentCount.get()));
            }
        }
        return counts;
    }

    public boolean isEmpty() { return mCounts.isEmpty(); }
}
