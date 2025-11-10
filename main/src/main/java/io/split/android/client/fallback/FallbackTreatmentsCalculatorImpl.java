package io.split.android.client.fallback;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import io.split.android.grammar.Treatments;

public final class FallbackTreatmentsCalculatorImpl implements FallbackTreatmentsCalculator {

    private static final String LABEL_PREFIX = "fallback - ";

    @NonNull
    private final FallbackTreatmentsConfiguration mConfig;

    public FallbackTreatmentsCalculatorImpl(@NonNull FallbackTreatmentsConfiguration config) {
        mConfig = checkNotNull(config);
    }

    @NonNull
    @Override
    public FallbackTreatment resolve(@NonNull String flagName) {
        return resolve(flagName, null);
    }

    @NonNull
    @Override
    public FallbackTreatment resolve(@NonNull String flagName, @Nullable String label) {
        Map<String, FallbackTreatment> byFlag = mConfig.getByFlag();
        if (byFlag != null) {
            FallbackTreatment flagTreatment = byFlag.get(flagName);
            if (flagTreatment != null) {
                return flagTreatment.copyWithLabel(resolveLabel(label));
            }
        }
        FallbackTreatment global = mConfig.getGlobal();
        if (global != null) {
            return global.copyWithLabel(resolveLabel(label));
        }
        return new FallbackTreatment(Treatments.CONTROL, null, label);
    }

    @Nullable
    private static String resolveLabel(@Nullable String label) {
        if (label == null) {
            return null;
        }

        return LABEL_PREFIX + label;
    }
}
