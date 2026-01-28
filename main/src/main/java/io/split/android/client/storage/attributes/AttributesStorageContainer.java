package io.split.android.client.storage.attributes;

import java.util.Map;

public interface AttributesStorageContainer {

    AttributesStorage getStorageForKey(String matchingKey);

    Map<String, AttributesStorage> getCurrentStorages();

    void destroy();
}
