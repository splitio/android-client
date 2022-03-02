package io.split.android.client.service.synchronizer.attributes;

public interface AttributesSynchronizerRegister {

    void registerAttributesSynchronizer(String userKey, AttributesSynchronizer attributesSynchronizer);

    void unregisterAttributesSynchronizer(String userKey);
}
