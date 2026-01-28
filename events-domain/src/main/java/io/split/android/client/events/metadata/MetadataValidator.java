package io.split.android.client.events.metadata;

import androidx.annotation.Nullable;

interface MetadataValidator {

    boolean isValidValue(@Nullable Object value);
}
