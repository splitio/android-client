package io.split.android.client.events.metadata;

import androidx.annotation.Nullable;

import java.util.List;

class MetadataValidatorImpl implements MetadataValidator {

    @Override
    public boolean isValidValue(@Nullable Object value) {
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
