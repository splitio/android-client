package io.split.android.client.fallback;

import androidx.annotation.Nullable;

interface FallbacksSanitizer {

    @Nullable
    FallbackTreatmentsConfiguration sanitize(@Nullable FallbackTreatmentsConfiguration config);
}
