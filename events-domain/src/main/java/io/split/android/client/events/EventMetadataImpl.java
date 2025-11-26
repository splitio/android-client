package io.split.android.client.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
 * Values are sanitized to only allow String, Number, Boolean, or List&lt;String&gt;.
 */
public class EventMetadataImpl implements EventMetadata {

    private final Map<String, Object> mData;

    public EventMetadataImpl(@NonNull Map<String, Object> data) {
        Map<String, Object> sanitized = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (isValidValue(entry.getValue())) {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }
        mData = Collections.unmodifiableMap(sanitized);
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
        return new HashMap<>(mData);
    }

    private boolean isValidValue(Object value) {
        if (value == null) {
            return false;
        }

        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return true;
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (Object item : list) {
                if (!(item instanceof String)) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }
}

