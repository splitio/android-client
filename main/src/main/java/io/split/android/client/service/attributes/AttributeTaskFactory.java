package io.split.android.client.service.attributes;

import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public interface AttributeTaskFactory {

    UpdateAttributesInPersistentStorageTask createAttributeUpdateTask(PersistentAttributesStorage persistentAttributesStorage);

    ClearAttributesInPersistentStorageTask createAttributeClearTask(PersistentAttributesStorage persistentAttributesStorage);

    LoadAttributesTask createLoadAttributesTask(PersistentAttributesStorage persistentAttributesStorage);
}
