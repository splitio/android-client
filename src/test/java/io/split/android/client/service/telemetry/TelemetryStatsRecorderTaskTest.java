package io.split.android.client.service.telemetry;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.Stats;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.telemetry.storage.TelemetryStatsProvider;

public class TelemetryStatsRecorderTaskTest {

    @Mock
    private HttpRecorder<Stats> recorder;
    @Mock
    private TelemetryStatsProvider statsProvider;
    @Mock
    private TelemetryRuntimeProducer telemetryRuntimeProducer;
    private TelemetryStatsRecorderTask telemetryConfigRecorderTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryConfigRecorderTask = new TelemetryStatsRecorderTask(recorder, statsProvider, telemetryRuntimeProducer);
    }

    @Test
    public void successfulExecutionClearsValuesOnProvider() {
        telemetryConfigRecorderTask.execute();

        verify(statsProvider).clearStats();
    }

    @Test
    public void executeFetchesStatsFromProvider() {

        telemetryConfigRecorderTask.execute();

        verify(statsProvider).getTelemetryStats();
    }

    @Test
    public void executeSendsStatsToRecorder() throws HttpRecorderException {
        Stats expectedStats = new Stats();
        when(statsProvider.getTelemetryStats()).thenReturn(expectedStats);
        ArgumentCaptor<Stats> captor = ArgumentCaptor.forClass(Stats.class);

        telemetryConfigRecorderTask.execute();

        verify(recorder).execute(captor.capture());
        assertEquals(expectedStats, captor.getValue());
    }

    @Test
    public void unsuccessfulExecutionDoesNotClearValuesOnProvider() {
        when(statsProvider.getTelemetryStats()).thenAnswer(invocation -> {
            throw new Exception("test exception");
        });

        telemetryConfigRecorderTask.execute();

        verify(statsProvider, times(0)).clearStats();
    }

    @Test
    public void latencyIsRecordedInTelemetry() {
        telemetryConfigRecorderTask.execute();

        verify(telemetryRuntimeProducer).recordSyncLatency(eq(OperationType.TELEMETRY), anyLong());
    }
}
