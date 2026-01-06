package io.split.android.client.events.metadata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.split.android.client.api.EventMetadata;

/**
 * Implementation of {@link EventMetadata}.
 * Use {@link EventMetadataHelpers} factory methods to create instances.
 */
public class EventMetadataImpl implements EventMetadata {

    @NonNull
    private final Type mType;
    @NonNull
    private final List<String> mValues;
    @Nullable
    private final Long mValue;

    /**
     * Creates a new EventMetadataImpl.
     *
     * @param type   the type of metadata
     * @param values the list of values (flag names, segment names, etc.)
     * @param value  the numeric value (changeNumber, timestamp, etc.)
     */
    public EventMetadataImpl(@NonNull Type type, @NonNull List<String> values, @Nullable Long value) {
        mType = type;
        // Defensive copy to ensure immutability
        mValues = Collections.unmodifiableList(new ArrayList<>(values));
        mValue = value;
    }

    @NonNull
    @Override
    public Type getType() {
        return mType;
    }

    @NonNull
    @Override
    public List<String> getValues() {
        return mValues;
    }

    @Nullable
    @Override
    public Long getValue() {
        return mValue;
    }
}
