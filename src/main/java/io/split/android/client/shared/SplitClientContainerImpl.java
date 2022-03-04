package io.split.android.client.shared;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientFactory;
import io.split.android.client.api.Key;

public class SplitClientContainerImpl implements SplitClientContainer {

    private final ConcurrentMap<String, SplitClient> mClientInstances = new ConcurrentHashMap<>();
    private final SplitClientFactory mSplitClientFactory;
    private final Object mClientCreationLock = new Object();

    public SplitClientContainerImpl(@NonNull SplitClientFactory splitClientFactory) {
        mSplitClientFactory = checkNotNull(splitClientFactory);
    }

    @Override
    public SplitClient getClient(Key key) {
        return getOrCreateClientForKey(key);
    }

    private SplitClient getOrCreateClientForKey(Key key) {
        synchronized (mClientCreationLock) {
            if (mClientInstances.get(key.matchingKey()) != null) {
                return mClientInstances.get(key.matchingKey());
            }

            boolean isDefaultClient = mClientInstances.isEmpty();
            SplitClient newClient = mSplitClientFactory.getClient(key, isDefaultClient);

            mClientInstances.put(key.matchingKey(), newClient);
        }

        return mClientInstances.get(key.matchingKey());
    }

    @Override
    public Set<SplitClient> getAll() {
        return new HashSet<>(mClientInstances.values());
    }
}
