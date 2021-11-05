package io.split.android.client.storage.attributes;

import java.util.Map;

public interface PersistentAttributesStorage {

    void set(Map<String, Object> attributes);

    Map<String, Object> get();

    void clear();
}
