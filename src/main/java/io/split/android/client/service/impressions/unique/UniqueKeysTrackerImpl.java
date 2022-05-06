package io.split.android.client.service.impressions.unique;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UniqueKeysTrackerImpl implements UniqueKeysTracker {

    private final Map<String, Set<String>> mCache;
    private final Object mLock = new Object();

    public UniqueKeysTrackerImpl() {
        mCache = new ConcurrentHashMap<>();
    }

    @Override
    public boolean track(String key, String featureName) {
        if (key == null || featureName == null) {
            return false;
        }

        synchronized (mLock) {
            if (!mCache.containsKey(key)) {
                mCache.put(key, new HashSet<>());
            }

            mCache.get(key).add(featureName);

            return true;
        }
    }

    @Override
    public Map<String, Set<String>> popAll() {
        synchronized (mLock) {
            Map<String, Set<String>> result = new HashMap<>(mCache);
            mCache.clear();

            return result;
        }
    }
}
