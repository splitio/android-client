package io.split.android.client.service.synchronizer.attributes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AttributesSynchronizerRegistryImpl implements AttributesSynchronizerRegistry, AttributesSynchronizer {

    private final AtomicBoolean mLoadedAttributesFromCache = new AtomicBoolean(false);
    private final ConcurrentMap<String, AttributesSynchronizer> mAttributesSynchronizers = new ConcurrentHashMap<>();

    @Override
    public synchronized void registerAttributesSynchronizer(String userKey, AttributesSynchronizer attributesSynchronizer) {
        mAttributesSynchronizers.put(userKey, attributesSynchronizer);
        if (mLoadedAttributesFromCache.get()) {
            attributesSynchronizer.loadAttributesFromCache();
        }
    }

    @Override
    public void unregisterAttributesSynchronizer(String userKey) {
        mAttributesSynchronizers.remove(userKey);
    }

    @Override
    public synchronized void loadAttributesFromCache() {
        for (AttributesSynchronizer attributesSynchronizer : mAttributesSynchronizers.values()) {
            attributesSynchronizer.loadAttributesFromCache();
        }

        mLoadedAttributesFromCache.set(true);
    }
}
