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

    private final ConcurrentMap<String, SplitClient> mClientInstances = new ConcurrentHashMap<>();
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
        synchronized (mClientCreationLock) {
            if (mClientInstances.get(key.matchingKey()) != null) {
                return mClientInstances.get(key.matchingKey());
            }

            createNewClient(key);
        }

        return mClientInstances.get(key.matchingKey());
    }

    @NonNull
    protected Set<String> getKeySet() {
        return mClientInstances.keySet();
    }

    protected void trackNewClient(String matchingKey, SplitClient client) {
        mClientInstances.put(matchingKey, client);
    }

    @ForOverride
    protected abstract void createNewClient(Key key);
}
