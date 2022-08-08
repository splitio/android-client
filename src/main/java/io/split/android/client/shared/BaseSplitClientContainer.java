package io.split.android.client.shared;

import androidx.annotation.NonNull;

import com.google.errorprone.annotations.ForOverride;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.split.android.client.SplitClient;
import io.split.android.client.api.Key;

public abstract class BaseSplitClientContainer implements SplitClientContainer {

    private final ConcurrentMap<Key, SplitClient> mClientInstances = new ConcurrentHashMap<>();
    private final Object mClientCreationLock = new Object();

    @Override
    public SplitClient getClient(Key key) {
        return getOrCreateClientForKey(key);
    }

    @Override
    public void remove(Key key) {
        mClientInstances.remove(key);
    }

    @Override
    public Set<SplitClient> getAll() {
        return new HashSet<>(mClientInstances.values());
    }

    private SplitClient getOrCreateClientForKey(Key key) {
        synchronized (mClientCreationLock) {
            if (mClientInstances.get(key) != null) {
                return mClientInstances.get(key);
            }

            createNewClient(key);
        }

        return mClientInstances.get(key);
    }

    @NonNull
    protected Set<String> getKeySet() {
        Set<Key> keys = mClientInstances.keySet();
        Set<String> matchingKeys = new HashSet<>();
        for (Key key : keys) {
            matchingKeys.add(key.matchingKey());
        }

        return matchingKeys;
    }

    protected void trackNewClient(Key key, SplitClient client) {
        mClientInstances.put(key, client);
    }

    @ForOverride
    protected abstract void createNewClient(Key key);
}
