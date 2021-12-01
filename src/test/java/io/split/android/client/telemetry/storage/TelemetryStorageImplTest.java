package io.split.android.client.telemetry.storage;

import static org.junit.Assert.*;

import com.google.common.util.concurrent.Runnables;

import org.junit.Test;

import io.split.android.client.service.synchronizer.ThreadUtils;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.model.MethodLatencies;

public class TelemetryStorageImplTest {

    private final TelemetryStorageImpl telemetryStorage = new TelemetryStorageImpl();

    @Test
    public void popExceptionsReturnsCorrectlyBuiltMethodExceptions() {
        telemetryStorage.recordException(Method.TRACK);
        telemetryStorage.recordException(Method.TREATMENT);
        telemetryStorage.recordException(Method.TREATMENT);
        telemetryStorage.recordException(Method.TREATMENTS);
        telemetryStorage.recordException(Method.TREATMENTS_WITH_CONFIG);
        telemetryStorage.recordException(Method.TREATMENT_WITH_CONFIG);

        MethodExceptions methodExceptions = telemetryStorage.popExceptions();

        assertEquals(1, methodExceptions.getTrack());
        assertEquals(2, methodExceptions.getTreatment());
        assertEquals(1, methodExceptions.getTreatments());
        assertEquals(1, methodExceptions.getTreatmentsWithConfig());
        assertEquals(1, methodExceptions.getTreatmentWithConfig());
    }

    @Test
    public void popExceptionsEmptiesCounters() {
        telemetryStorage.recordException(Method.TRACK);
        telemetryStorage.recordException(Method.TREATMENT);
        telemetryStorage.recordException(Method.TREATMENT);
        telemetryStorage.recordException(Method.TREATMENTS);
        telemetryStorage.recordException(Method.TREATMENTS_WITH_CONFIG);
        telemetryStorage.recordException(Method.TREATMENT_WITH_CONFIG);

        telemetryStorage.popExceptions();

        MethodExceptions secondPop = telemetryStorage.popExceptions();

        assertEquals(0, secondPop.getTrack());
        assertEquals(0, secondPop.getTreatment());
        assertEquals(0, secondPop.getTreatments());
        assertEquals(0, secondPop.getTreatmentsWithConfig());
        assertEquals(0, secondPop.getTreatmentWithConfig());
    }

    @Test
    public void popLatenciesReturnsCorrectlyBuiltObject() {
        telemetryStorage.recordLatency(Method.TRACK, 200);
        telemetryStorage.recordLatency(Method.TREATMENT, 10022);
        telemetryStorage.recordLatency(Method.TREATMENT, 300);
        telemetryStorage.recordLatency(Method.TREATMENTS, 200);
        telemetryStorage.recordLatency(Method.TREATMENTS_WITH_CONFIG, 10);
        telemetryStorage.recordLatency(Method.TREATMENT_WITH_CONFIG, 2000);

        MethodLatencies methodLatencies = telemetryStorage.popLatencies();

        assertFalse(methodLatencies.getTrack().stream().allMatch(l -> l == 0));
        assertFalse(methodLatencies.getTreatment().stream().allMatch(l -> l == 0));
        assertFalse(methodLatencies.getTreatments().stream().allMatch(l -> l == 0));
        assertFalse(methodLatencies.getTreatmentsWithConfig().stream().allMatch(l -> l == 0));
        assertFalse(methodLatencies.getTreatmentWithConfig().stream().allMatch(l -> l == 0));
    }
}
