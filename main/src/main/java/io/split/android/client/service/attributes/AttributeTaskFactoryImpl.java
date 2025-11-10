package io.split.android.client.service.attributes;

import java.util.Map;

import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class AttributeTaskFactoryImpl implements AttributeTaskFactory {

    private final String mMatchingKey;
    private final AttributesStorage mAttributesStorage;

    public AttributeTaskFactoryImpl(String matchingKey, AttributesStorage attributesStorage) {
        mMatchingKey = matchingKey;
        mAttributesStorage = attributesStorage;
    }

    @Override
    public UpdateAttributesInPersistentStorageTask createAttributeUpdateTask(PersistentAttributesStorage persistentAttributesStorage, Map<String, Object> attributes) {
        return new UpdateAttributesInPersistentStorageTask(mMatchingKey, persistentAttributesStorage, attributes);
    }

    @Override
    public ClearAttributesInPersistentStorageTask createAttributeClearTask(PersistentAttributesStorage persistentAttributesStorage) {
        return new ClearAttributesInPersistentStorageTask(mMatchingKey, persistentAttributesStorage);
    }

    @Override
    public LoadAttributesTask createLoadAttributesTask(PersistentAttributesStorage persistentAttributesStorage) {
        return new LoadAttributesTask(mMatchingKey, mAttributesStorage, persistentAttributesStorage);
    }
}
