package io.split.android.client.service.attributes;

import androidx.annotation.NonNull;

import io.split.android.client.storage.attributes.AttributesStorage;

public class UpdateSingleAttributeTask extends GenericUpdateAttributeTask {

    public UpdateSingleAttributeTask(@NonNull AttributesStorage attributesStorage,
                                     @NonNull String attributeName,
                                     @NonNull Object attributeValue) {
        super(attributesStorage, attributeName, attributeValue);
    }
}
