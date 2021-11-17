package io.split.android.client.service.attributes;

import java.util.Map;

import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public interface AttributeTaskFactory {

    AttributeUpdateTask createAttributeUpdateTask(PersistentAttributesStorage persistentAttributesStorage, Map<String, Object> attributes);

    AttributeClearTask createAttributeClearTask(PersistentAttributesStorage persistentAttributesStorage);
}
