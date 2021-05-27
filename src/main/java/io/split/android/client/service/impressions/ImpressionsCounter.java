package io.split.android.client.service.impressions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImpressionsCounter {

    public static class Key {
        private final String mFeatureName;
        private final long mTimeFrame;

        public Key(String featureName, long timeframe) {
            mFeatureName = checkNotNull(featureName);
            mTimeFrame = timeframe;
        }

        public String featureName() { return mFeatureName; }
        public long timeFrame() { return mTimeFrame; }

        @Override
        public int hashCode() {
            return String.format("%s%d", mFeatureName, mTimeFrame).hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;
            return mFeatureName.equals(key.mFeatureName) &&
                    mTimeFrame == key.mTimeFrame;
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

    public Map<Key, Integer> popAll() {
        HashMap<Key, Integer> toReturn = new HashMap<>();
        for (Key key : mCounts.keySet()) {
            AtomicInteger current = mCounts.remove(key);
            // It shouldn't be null...
            if(current != null) {
                toReturn.put(key, current.get());
            }
        }
        return toReturn;
    }

    public boolean isEmpty() { return mCounts.isEmpty(); }
}
