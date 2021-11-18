package io.split.android.client.service.attributes;

import java.util.Map;

import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class AttributeTaskFactoryImpl implements AttributeTaskFactory {

    @Override
    public UpdateAttributesInPersistentStorageTask createAttributeUpdateTask(PersistentAttributesStorage persistentAttributesStorage, Map<String, Object> attributes) {
        return new UpdateAttributesInPersistentStorageTask(persistentAttributesStorage, attributes);
    }

    @Override
    public ClearAttributesInPersistentStorageTask createAttributeClearTask(PersistentAttributesStorage persistentAttributesStorage) {
        return new ClearAttributesInPersistentStorageTask(persistentAttributesStorage);
    }
}
