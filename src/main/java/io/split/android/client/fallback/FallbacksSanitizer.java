package io.split.android.client.fallback;

import androidx.annotation.Nullable;

interface FallbacksSanitizer {

    @Nullable
    FallbackConfiguration sanitize(@Nullable FallbackConfiguration config);
}
