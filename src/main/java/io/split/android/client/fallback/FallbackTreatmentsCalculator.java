package io.split.android.client.fallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Resolves a fallback treatment for a given flag name.
 * Returns null if no fallback applies (caller should use control).
 */
public interface FallbackTreatmentsCalculator {

    /**
     * Resolve a fallback for a given flag name.
     * @param flagName non-null flag name
     * @return a fallback treatment with a null label, if configured; otherwise "control"
     */
    @NonNull
    FallbackTreatment resolve(@NonNull String flagName);

    /**
     * Resolve a fallback for a given flag name and label.
     *
     * @param flagName non-null flag name
     * @param label    nullable label
     * @return a fallback treatment if configured, with a prefixed label if provided; otherwise "control"
     */
    @NonNull
    FallbackTreatment resolve(@NonNull String flagName, @Nullable String label);
}
