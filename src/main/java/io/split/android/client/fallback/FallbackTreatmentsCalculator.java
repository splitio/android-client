package io.split.android.client.fallback;

import androidx.annotation.NonNull;

/**
 * Resolves a fallback treatment for a given flag name.
 * Returns null if no fallback applies (caller should use control).
 */
public interface FallbackTreatmentsCalculator {

    /**
     * Resolve a fallback for a given flag name.
     *
     * @param flagName non-null flag name
     * @return a fallback treatment if configured, otherwise "control"
     */
    @NonNull
    FallbackTreatment resolve(@NonNull String flagName);
}
