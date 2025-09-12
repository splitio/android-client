package io.split.android.client.fallback;

import androidx.annotation.Nullable;
import java.util.Map;

interface FallbacksSanitizer {

    @Nullable
    FallbackTreatment sanitizeGlobal(@Nullable FallbackTreatment global);

    Map<String, FallbackTreatment> sanitizeByFlag(@Nullable Map<String, FallbackTreatment> byFlag);
}
