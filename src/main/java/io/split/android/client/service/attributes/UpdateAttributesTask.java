package io.split.android.client.service.attributes;

import androidx.annotation.NonNull;

import java.util.Map;

import io.split.android.client.storage.attributes.AttributesStorage;

public class UpdateAttributesTask extends GenericUpdateAttributeTask {

    public UpdateAttributesTask(@NonNull AttributesStorage attributesStorage,
                                @NonNull Map<String, Object> attributes) {
        super(attributesStorage, attributes);
    }
}
