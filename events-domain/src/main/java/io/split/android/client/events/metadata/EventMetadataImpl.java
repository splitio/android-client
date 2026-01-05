package io.split.android.client.events.metadata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.api.EventMetadata;
import io.split.android.client.api.MetadataKey;

/**
 * Implementation of {@link EventMetadata}.
 * Use {@link EventMetadataBuilder} to create instances.
 */
class EventMetadataImpl implements EventMetadata {

    private final Map<String, Object> mData;

    EventMetadataImpl(@NonNull Map<String, Object> data) {
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List) {
                copy.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>((List<?>) value)));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        mData = Collections.unmodifiableMap(copy);
    }

    @Override
    public int size() {
        return mData.size();
    }

    @NonNull
    @Override
    public Collection<Object> values() {
        return mData.values();
    }

    @Nullable
    @Override
    public <T> T get(@NonNull MetadataKey<T> key) {
        //noinspection unchecked
        return (T) mData.get(key.name());
    }

    @Override
    public boolean containsKey(@NonNull MetadataKey<?> key) {
        return mData.containsKey(key.name());
    }
}
