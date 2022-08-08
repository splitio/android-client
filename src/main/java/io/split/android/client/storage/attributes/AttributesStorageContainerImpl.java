package io.split.android.client.storage.attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AttributesStorageContainerImpl implements AttributesStorageContainer {

    private final ConcurrentMap<String, AttributesStorage> mStorageMap = new ConcurrentHashMap<>();
    private final Object mLock = new Object();

    @Override
    public AttributesStorage getStorageForKey(String matchingKey) {
        synchronized (mLock) {
            if (mStorageMap.get(matchingKey) == null) {
                mStorageMap.put(matchingKey, new AttributesStorageImpl());
            }

            return mStorageMap.get(matchingKey);
        }
    }

    @Override
    public Map<String, AttributesStorage> getCurrentStorages() {
        return new HashMap<>(mStorageMap);
    }

    @Override
    public void destroy() {
        mStorageMap.clear();
    }
}
