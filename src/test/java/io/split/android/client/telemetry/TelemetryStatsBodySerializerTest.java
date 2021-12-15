package io.split.android.client.telemetry;

import static junit.framework.TestCase.assertEquals;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.telemetry.model.Stats;

public class TelemetryStatsBodySerializerTest {

    private TelemetryStatsBodySerializer telemetryStatsBodySerializer;

    @Before
    public void setUp() {
        telemetryStatsBodySerializer = new TelemetryStatsBodySerializer();
    }

    @Test
    public void jsonIsBuiltAsExpected() {
        String serializedStats = telemetryStatsBodySerializer.serialize(getMockStats());

        assertEquals("{}", serializedStats);
    }

    private Stats getMockStats() {
        return new Stats();
    }
}
