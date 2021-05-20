package io.split.android.client.service.impressions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImpressionCounter {

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
            return Objects.hash(mFeatureName, mTimeFrame);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;
            return Objects.equals(mFeatureName, key.mFeatureName) &&
                    Objects.equals(mTimeFrame, key.mTimeFrame);
        }
    }

    private final ConcurrentHashMap<Key, AtomicInteger> mCounts;

    public ImpressionCounter() {
        mCounts = new ConcurrentHashMap<>();
    }

    public void inc(String featureName, long timeFrame, int amount) {
        Key key = new Key(featureName, ImpressionUtils.truncateTimeframe(timeFrame));
        AtomicInteger count = mCounts.get(key);
        if (Objects.isNull(count)) {
            count = new AtomicInteger();
            AtomicInteger old = mCounts.putIfAbsent(key, count);
            if (!Objects.isNull(old)) { // Some other thread won the race, use that AtomicInteger instead
                count = old;
            }
        }
        count.addAndGet(amount);
    }

    public Map<Key, Integer> popAll() {
        HashMap<Key, Integer> toReturn = new HashMap<>();
        for (Key key : mCounts.keySet()) {
            AtomicInteger curr = mCounts.remove(key);
            toReturn.put(key, curr.get());
        }
        return toReturn;
    }

    public boolean isEmpty() { return mCounts.isEmpty(); }
}
