package io.split.android.client.attributes;

import io.split.android.client.storage.attributes.AttributesStorage;

public interface AttributesManagerFactory {

    AttributesManager getManager(String matchingKey, AttributesStorage attributesStorage);
}
