package io.split.android.client.fallback;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.split.android.client.utils.logger.Logger;

/**
 * Validates and sanitizes fallback configurations by applying validation rules.
 * Invalid entries are dropped and warnings are logged.
 */
class FallbacksSanitizerImpl implements FallbacksSanitizer {

    private static final int MAX_FLAG_NAME_LENGTH = 100;
    private static final int MAX_TREATMENT_LENGTH = 100;
    private static final String TREATMENT_REGEXP = "^[0-9]+[.a-zA-Z0-9_-]*$|^[a-zA-Z]+[a-zA-Z0-9_-]*$";
    private static final Pattern TREATMENT_PATTERN = Pattern.compile(TREATMENT_REGEXP);

    /**
     * Sanitizes the provided fallback configuration by applying validation rules.
     * Invalid entries are dropped and warnings are logged.
     *
     * @param config the configuration to sanitize; may be null
     * @return a new sanitized configuration, or null if input was null
     */
    @Nullable
    public FallbackConfiguration sanitize(@Nullable FallbackConfiguration config) {
        if (config == null) {
            return null;
        }

        // Sanitize global treatment
        FallbackTreatment sanitizedGlobal = sanitizeGlobalTreatment(config.getGlobal());

        // Sanitize by-flag treatments
        Map<String, FallbackTreatment> sanitizedByFlag = sanitizeByFlagTreatments(config.getByFlag());

        return FallbackConfiguration.builder()
                .global(sanitizedGlobal)
                .byFlag(sanitizedByFlag)
                .build();
    }

    @Nullable
    private FallbackTreatment sanitizeGlobalTreatment(@Nullable FallbackTreatment global) {
        if (global == null) {
            return null;
        }

        if (!isValidTreatment(global)) {
            Logger.e("Discarded global fallback: Invalid treatment (max " + MAX_TREATMENT_LENGTH + " chars and comply with " + TREATMENT_REGEXP + ")");
            return null;
        }

        return global;
    }

    private Map<String, FallbackTreatment> sanitizeByFlagTreatments(Map<String, FallbackTreatment> byFlag) {
        if (byFlag == null || byFlag.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, FallbackTreatment> sanitized = new HashMap<>();

        for (Map.Entry<String, FallbackTreatment> entry : byFlag.entrySet()) {
            String flagName = entry.getKey();
            FallbackTreatment treatment = entry.getValue();

            if (!isValidFlagName(flagName)) {
                Logger.e("Discarded flag '" + flagName + "': Invalid flag name (max " + MAX_FLAG_NAME_LENGTH + " chars, no spaces)");
                continue;
            }

            if (!isValidTreatment(treatment)) {
                Logger.e("Discarded treatment for flag '" + flagName + "': Invalid treatment (max " + MAX_TREATMENT_LENGTH + " chars and comply with " + TREATMENT_REGEXP + ")");
                continue;
            }

            sanitized.put(flagName, treatment);
        }

        return sanitized;
    }

    private static boolean isValidFlagName(String flagName) {
        if (flagName == null) {
            return false;
        }
        return flagName.length() <= MAX_FLAG_NAME_LENGTH && !flagName.contains(" ");
    }

    private static boolean isValidTreatment(FallbackTreatment treatment) {
        if (treatment == null || treatment.getTreatment() == null) {
            return false;
        }
        String value = treatment.getTreatment();
        return value.length() <= MAX_TREATMENT_LENGTH && TREATMENT_PATTERN.matcher(value).matches();
    }
}
