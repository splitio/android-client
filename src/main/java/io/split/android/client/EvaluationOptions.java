package io.split.android.client;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class EvaluationOptions {

    private final Map<String, Object> mProperties;

    public EvaluationOptions(Map<String, Object> properties) {
        mProperties = properties != null ? new HashMap<>(properties) : null;
    }

    public Map<String, Object> getProperties() {
        return mProperties != null ? new HashMap<>(mProperties) : null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof EvaluationOptions)) {
            return false;
        }
        EvaluationOptions other = (EvaluationOptions) obj;
        if (mProperties == null) {
            return other.mProperties == null;
        }
        return mProperties.equals(other.mProperties);
    }

    @Override
    public int hashCode() {
        return mProperties != null ? mProperties.hashCode() : 0;
    }
}
