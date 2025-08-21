package io.split.android.client.fallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Represents the fallback treatment, with an optional config and a fixed label.
 */
public final class FallbackTreatment {

    public static final String LABEL = "fallback treatment";

    @NonNull
    private final String mTreatment;
    @Nullable
    private final String mConfig;

    public FallbackTreatment(@NonNull String treatment) {
        this(treatment, null);
    }

    public FallbackTreatment(@NonNull String treatment, @Nullable String config) {
        mTreatment = treatment;
        mConfig = config;
    }

    public String getTreatment() {
        return mTreatment;
    }

    @Nullable
    public String getConfig() {
        return mConfig;
    }

    public String getLabel() {
        return LABEL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FallbackTreatment that = (FallbackTreatment) o;
        return Objects.equals(mTreatment, that.mTreatment) &&
                Objects.equals(mConfig, that.mConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTreatment, mConfig);
    }

    @Override
    public String toString() {
        return "FallbackTreatment{" +
                "treatment='" + mTreatment + '\'' +
                ", config=" + (mConfig == null ? "null" : "[redacted]") +
                ", label='" + LABEL + '\'' +
                '}';
    }
}
