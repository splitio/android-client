package io.split.android.client.service.synchronizer.attributes;

public interface AttributesSynchronizerRegistry {

    void registerAttributesSynchronizer(String userKey, AttributesSynchronizer attributesSynchronizer);

    void unregisterAttributesSynchronizer(String userKey);
}
