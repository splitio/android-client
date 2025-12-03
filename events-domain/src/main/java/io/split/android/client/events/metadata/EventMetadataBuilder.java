package io.split.android.client.events.metadata;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.api.EventMetadata;

/**
 * Builder for creating {@link EventMetadata} instances.
 * <p>
 * Values are validated during put operations. Only String, Number, Boolean,
 * and List&lt;String&gt; values are accepted. Invalid values will be silently ignored.
 */
class EventMetadataBuilder {

    private static final MetadataValidator DEFAULT_VALIDATOR = new MetadataValidatorImpl();

    private final Map<String, Object> mData = new HashMap<>();
    private final MetadataValidator mValidator;

    EventMetadataBuilder() {
        this(DEFAULT_VALIDATOR);
    }

    @VisibleForTesting
    EventMetadataBuilder(@NonNull MetadataValidator validator) {
        mValidator = validator;
    }

    /**
     * Adds a String value to the metadata.
     *
     * @param key   the key
     * @param value the String value
     * @return this builder
     */
    @NonNull
    public EventMetadataBuilder put(@NonNull String key, @NonNull String value) {
        if (mValidator.isValidValue(value)) {
            mData.put(key, value);
        }
        return this;
    }

    /**
     * Adds a Number value to the metadata.
     *
     * @param key   the key
     * @param value the Number value (Integer, Long, Double, Float, etc.)
     * @return this builder
     */
    @NonNull
    public EventMetadataBuilder put(@NonNull String key, @NonNull Number value) {
        if (mValidator.isValidValue(value)) {
            mData.put(key, value);
        }
        return this;
    }

    /**
     * Adds a Boolean value to the metadata.
     *
     * @param key   the key
     * @param value the Boolean value
     * @return this builder
     */
    @NonNull
    public EventMetadataBuilder put(@NonNull String key, boolean value) {
        if (mValidator.isValidValue(value)) {
            mData.put(key, value);
        }
        return this;
    }

    /**
     * Adds a List of Strings to the metadata.
     *
     * @param key   the key
     * @param value the list of strings
     * @return this builder
     */
    @NonNull
    public EventMetadataBuilder put(@NonNull String key, @NonNull List<String> value) {
        if (mValidator.isValidValue(value)) {
            mData.put(key, value);
        }
        return this;
    }

    /**
     * Builds the {@link EventMetadata} instance.
     *
     * @return a new immutable EventMetadata instance
     */
    @NonNull
    public EventMetadata build() {
        return new EventMetadataImpl(mData);
    }
}
