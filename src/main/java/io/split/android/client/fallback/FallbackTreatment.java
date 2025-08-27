package io.split.android.client.fallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import io.split.android.grammar.Treatments;

/**
 * Represents the fallback treatment, with an optional config and a fixed label.
 */
public final class FallbackTreatment {

    /**
     * Default fallback representing "control" treatment with no config.
     */
    public static final FallbackTreatment CONTROL = new FallbackTreatment(Treatments.CONTROL);

    @NonNull
    private final String mTreatment;
    @Nullable
    private final String mConfig;
    @Nullable
    private final String mLabel;

    public FallbackTreatment(@NonNull String treatment) {
        this(treatment, null);
    }

    public FallbackTreatment(@NonNull String treatment, @Nullable String config) {
        this(treatment, config, null);
    }

    private FallbackTreatment(@NonNull String treatment, @Nullable String config, @Nullable String label) {
        mTreatment = treatment;
        mConfig = config;
        mLabel = label;
    }

    public String getTreatment() {
        return mTreatment;
    }

    @Nullable
    public String getConfig() {
        return mConfig;
    }

    @Nullable
    public String getLabel() {
        return mLabel;
    }

    FallbackTreatment copyWithLabel(String label) {
        return new FallbackTreatment(mTreatment, mConfig, label);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FallbackTreatment that = (FallbackTreatment) o;
        return Objects.equals(mTreatment, that.mTreatment) &&
                Objects.equals(mConfig, that.mConfig) &&
                Objects.equals(mLabel, that.mLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTreatment, mConfig, mLabel);
    }
}
