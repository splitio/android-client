package io.split.android.telemetry.storage;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

import io.split.android.telemetry.model.Method;
import io.split.android.telemetry.model.MethodExceptions;

public class TelemetryStorageImplTest {

    private TelemetryStorageImpl telemetryStorage = new TelemetryStorageImpl();

    @Test
    public void popExceptionsReturnsCorrectlyBuiltMethodExceptions() {
        telemetryStorage.recordException(Method.TRACK);
        telemetryStorage.recordException(Method.TREATMENT);
        telemetryStorage.recordException(Method.TREATMENT);
        telemetryStorage.recordException(Method.TREATMENTS_WITH_CONFIG);

        MethodExceptions methodExceptions = telemetryStorage.popExceptions();

        assertEquals(1, methodExceptions.getTrack());
        assertEquals(2, methodExceptions.getTreatment());
        assertEquals(0, methodExceptions.getTreatments());
        assertEquals(1, methodExceptions.getTreatmentsWithConfig());
        assertEquals(0, methodExceptions.getTreatmentWithConfig());
    }

    @Test
    public void popLatencies() {
    }

    @Test
    public void recordLatency() {
    }

    @Test
    public void recordException() {
    }
}