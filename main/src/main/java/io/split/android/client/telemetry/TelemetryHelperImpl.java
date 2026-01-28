package io.split.android.client.telemetry;

public class TelemetryHelperImpl implements TelemetryHelper {

    private static final double MAX_VALUE_PROBABILITY = 1000;
    private static final double ACCEPTANCE_RANGE = 0.001;

    @Override
    public boolean shouldRecordTelemetry() {
        return Math.random() * (MAX_VALUE_PROBABILITY + 1) / MAX_VALUE_PROBABILITY <= ACCEPTANCE_RANGE;
    }
}
