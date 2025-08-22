package io.split.android.client.fallback;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

public final class FallbackTreatmentsConfiguration {

    @Nullable
    private final FallbackConfiguration mByFactory;

    private FallbackTreatmentsConfiguration(@Nullable FallbackConfiguration byFactory) {
        mByFactory = byFactory;
    }

    @Nullable
    public FallbackConfiguration getByFactory() {
        return mByFactory;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        @Nullable
        private FallbackConfiguration mByFactory;
        private FallbacksSanitizer mSanitizer;

        private Builder() {
            mSanitizer = new FallbacksSanitizerImpl();
        }

        @VisibleForTesting
        Builder sanitizer(FallbacksSanitizer sanitizer) {
            mSanitizer = sanitizer;
            return this;
        }

        public Builder byFactory(@Nullable FallbackConfiguration byFactory) {
            mByFactory = byFactory;
            return this;
        }

        public FallbackTreatmentsConfiguration build() {
            FallbackConfiguration sanitized = (mByFactory == null)
                    ? null
                    : mSanitizer.sanitize(mByFactory);
            return new FallbackTreatmentsConfiguration(sanitized);
        }
    }
}
