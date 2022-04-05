package io.split.android.client.service.attributes;

import java.util.Map;

import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public interface AttributeTaskFactory {

    UpdateAttributesInPersistentStorageTask createAttributeUpdateTask(PersistentAttributesStorage persistentAttributesStorage, Map<String, Object> attributes);

    ClearAttributesInPersistentStorageTask createAttributeClearTask(PersistentAttributesStorage persistentAttributesStorage);

    LoadAttributesTask createLoadAttributesTask(PersistentAttributesStorage persistentAttributesStorage);
}
