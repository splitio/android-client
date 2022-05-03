package io.split.android.client.shared;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.google.errorprone.annotations.ForOverride;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.split.android.client.SplitClient;
import io.split.android.client.api.Key;

public abstract class BaseSplitClientContainer implements SplitClientContainer {

    private final ConcurrentMap<Pair<String, String>, SplitClient> mClientInstances = new ConcurrentHashMap<>();
    private final Object mClientCreationLock = new Object();

    @Override
    public SplitClient getClient(Key key) {
        return getOrCreateClientForKey(key);
    }

    @Override
    public void remove(String key) {
        mClientInstances.remove(key);
    }

    @Override
    public Set<SplitClient> getAll() {
        return new HashSet<>(mClientInstances.values());
    }

    private SplitClient getOrCreateClientForKey(Key key) {
        Pair<String, String> pair = new Pair<>(key.matchingKey(), key.bucketingKey());
        synchronized (mClientCreationLock) {
            if (mClientInstances.get(pair) != null) {
                return mClientInstances.get(pair);
            }

            createNewClient(key);
        }

        return mClientInstances.get(pair);
    }

    @NonNull
    protected Set<String> getKeySet() {
        Set<Pair<String, String>> pairs = mClientInstances.keySet();
        Set<String> matchingKeys = new HashSet<>();
        for (Pair<String, String> pair : pairs) {
            matchingKeys.add(pair.first);
        }

        return matchingKeys;
    }

    protected void trackNewClient(String matchingKey, String bucketingKey, SplitClient client) {
        mClientInstances.put(new Pair<>(matchingKey, bucketingKey), client);
    }

    @ForOverride
    protected abstract void createNewClient(Key key);
}
