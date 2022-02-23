package io.split.android.client.storage.attributes;

public interface AttributesStorageContainer {

    AttributesStorage getStorageForKey(String matchingKey);

    void destroy();
}
