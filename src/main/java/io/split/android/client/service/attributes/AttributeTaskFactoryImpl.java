package io.split.android.client.service.attributes;

import java.util.Map;

import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class AttributeTaskFactoryImpl implements AttributeTaskFactory {

    @Override
    public AttributeUpdateTask createAttributeUpdateTask(PersistentAttributesStorage persistentAttributesStorage, Map<String, Object> attributes) {
        return new AttributeUpdateTask(persistentAttributesStorage, attributes);
    }

    @Override
    public AttributeClearTask createAttributeClearTask(PersistentAttributesStorage persistentAttributesStorage) {
        return new AttributeClearTask(persistentAttributesStorage);
    }
}
