package io.split.android.client.fallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.split.android.client.utils.logger.Logger;

public final class FallbackTreatmentsConfiguration {

    @Nullable
    private final FallbackTreatment mGlobal;
    private final Map<String, FallbackTreatment> mByFlag;

    private FallbackTreatmentsConfiguration(@Nullable FallbackTreatment global,
                                            @Nullable Map<String, FallbackTreatment> byFlag) {
        mGlobal = global;
        if (byFlag == null || byFlag.isEmpty()) {
            mByFlag = Collections.emptyMap();
        } else {
            mByFlag = Collections.unmodifiableMap(new HashMap<>(byFlag));
        }
    }

    @Nullable
    public FallbackTreatment getGlobal() {
        return mGlobal;
    }

    public Map<String, FallbackTreatment> getByFlag() {
        return mByFlag;
    }

    /**
     * Creates a new {@link Builder} for {@link FallbackTreatmentsConfiguration}.
     * Use this to provide an optional global fallback and flag-specific fallbacks.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        @Nullable
        private FallbackTreatment mGlobal;
        @Nullable
        private Map<String, FallbackTreatment> mByFlag;
        private FallbacksSanitizer mSanitizer;

        private Builder() {
            mGlobal = null;
            mByFlag = null;
            mSanitizer = new FallbacksSanitizerImpl();
        }

        /**
         * Sets an optional global fallback treatment to be used when no flag-specific
         * fallback exists for a given flag. This value is returned only in place of
         * the "control" treatment.
         *
         * @param global optional global {@link FallbackTreatment}
         * @return this builder instance
         */
        public Builder global(@Nullable FallbackTreatment global) {
            if (mGlobal != null && global != null) {
                Logger.w("Fallback treatments - You had previously set a global fallback. The new value will replace it");
            }
            mGlobal = global;
            return this;
        }

        /**
         * Sets an optional global fallback treatment to be used when no flag-specific
         * fallback exists for a given flag. This value is returned only in place of
         * the "control" treatment.
         *
         * @param treatment the treatment string to use as global
         * @return this builder instance
         */
        public Builder global(String treatment) {
            if (mGlobal != null) {
                Logger.w("Fallback treatments - You had previously set a global fallback. The new value will replace it");
            }
            mGlobal = new FallbackTreatment(treatment);
            return this;
        }

        /**
         * Sets optional flag-specific fallback treatments, where keys are flag names.
         * These take precedence over the global fallback.
         *
         * @param byFlag map of flag name to {@link FallbackTreatment}; may be null or empty
         * @return this builder instance
         */
        public Builder byFlag(@Nullable Map<String, FallbackTreatment> byFlag) {
            if (byFlag == null || byFlag.isEmpty()) {
                return this;
            }
            if (mByFlag == null) {
                mByFlag = new HashMap<>();
            }
            for (Map.Entry<String, FallbackTreatment> e : byFlag.entrySet()) {
                String key = e.getKey();
                if (mByFlag.containsKey(key)) {
                    Logger.w(getDuplicateFlagMessage(key));
                }
                mByFlag.put(key, e.getValue());
            }
            return this;
        }

        /**
         * Sets optional flag-specific fallback treatments, where keys are flag names.
         * These take precedence over the global fallback.
         *
         * @param byFlag map of flag name to treatment string; may be null or empty
         * @return this builder instance
         */
        public Builder byFlagStrings(@Nullable Map<String, String> byFlag) {
            if (byFlag == null || byFlag.isEmpty()) {
                return this;
            }
            if (mByFlag == null) {
                mByFlag = new HashMap<>();
            }
            for (Map.Entry<String, String> e : byFlag.entrySet()) {
                String key = e.getKey();
                if (mByFlag.containsKey(key)) {
                    Logger.w(getDuplicateFlagMessage(key));
                }
                mByFlag.put(key, new FallbackTreatment(e.getValue()));
            }
            return this;
        }

        /**
         * Builds a {@link FallbackTreatmentsConfiguration} for the configured values.
         *
         * @return a new immutable {@link FallbackTreatmentsConfiguration}
         */
        public FallbackTreatmentsConfiguration build() {
            FallbackTreatment sanitizedGlobal = mSanitizer.sanitizeGlobal(mGlobal);
            Map<String, FallbackTreatment> sanitizedByFlag = mSanitizer.sanitizeByFlag(mByFlag);
            return new FallbackTreatmentsConfiguration(sanitizedGlobal, sanitizedByFlag);
        }

        @VisibleForTesting
        Builder sanitizer(FallbacksSanitizer sanitizer) {
            mSanitizer = sanitizer;
            return this;
        }

        @NonNull
        private static String getDuplicateFlagMessage(String key) {
            return "Fallback treatments - Duplicate fallback for flag '" + key + "'. Overriding existing value.";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FallbackTreatmentsConfiguration that = (FallbackTreatmentsConfiguration) o;
        return Objects.equals(mGlobal, that.mGlobal) &&
                Objects.equals(mByFlag, that.mByFlag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mGlobal, mByFlag);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FallbackTreatmentsConfiguration that = (FallbackTreatmentsConfiguration) o;
        return Objects.equals(mGlobal, that.mGlobal) &&
                Objects.equals(mByFlag, that.mByFlag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mGlobal, mByFlag);
    }
}
