package io.split.android.client.fallback;

import androidx.annotation.NonNull;

import java.util.Map;

public final class FallbackTreatmentsCalculatorImpl implements FallbackTreatmentsCalculator {

    @NonNull
    private final FallbackConfiguration mConfig;

    public FallbackTreatmentsCalculatorImpl(@NonNull FallbackConfiguration config) {
        mConfig = config;
    }

    @NonNull
    @Override
    public FallbackTreatment resolve(@NonNull String flagName) {
        Map<String, FallbackTreatment> byFlag = mConfig.getByFlag();
        if (byFlag != null) {
            FallbackTreatment flagTreatment = byFlag.get(flagName);
            if (flagTreatment != null) {
                return flagTreatment;
            }
        }
        FallbackTreatment global = mConfig.getGlobal();
        if (global != null) {
            return global;
        }
        return FallbackTreatment.CONTROL;
    }
}
