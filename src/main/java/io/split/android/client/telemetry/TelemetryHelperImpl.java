package io.split.android.client.telemetry;

import java.util.Random;

public class TelemetryHelperImpl implements TelemetryHelper {

    private static final double MAX_VALUE_PROBABILITY = 1000;
    private static final double ACCEPTANCE_RANGE = 0.001;

    private final Random mRandom = new Random();

    @Override
    public boolean shouldRecordTelemetry() {
        return MAX_VALUE_PROBABILITY * mRandom.nextDouble() < ACCEPTANCE_RANGE;
    }
}
