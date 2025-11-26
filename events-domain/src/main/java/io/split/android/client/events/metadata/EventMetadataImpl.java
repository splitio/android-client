package io.split.android.client.events.metadata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.api.EventMetadata;

/**
 * Implementation of {@link EventMetadata}.
 * <p>
 * This class is immutable. Lists are defensively copied during construction
 * and wrapped as unmodifiable to prevent external mutation.
 * <p>
 * Use {@link EventMetadataBuilder} to create instances.
 */
class EventMetadataImpl implements EventMetadata {

    private final Map<String, Object> mData;

    EventMetadataImpl(@NonNull Map<String, Object> data) {
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            // Defensive copy for lists to ensure immutability
            if (value instanceof List) {
                copy.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>((List<?>) value)));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        mData = Collections.unmodifiableMap(copy);
    }

    @NonNull
    @Override
    public Set<String> keys() {
        return mData.keySet();
    }

    @NonNull
    @Override
    public Collection<Object> values() {
        return mData.values();
    }

    @Nullable
    @Override
    public Object get(@NonNull String key) {
        return mData.get(key);
    }

    @Override
    public boolean containsKey(@NonNull String key) {
        return mData.containsKey(key);
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : mData.entrySet()) {
            Object value = entry.getValue();
            // Return mutable copies of lists so callers can modify their copy
            if (value instanceof List) {
                copy.put(entry.getKey(), new ArrayList<>((List<?>) value));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }
}
