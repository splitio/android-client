package io.split.android.client.service.telemetry;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
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
    private TelemetryStatsRecorderTask telemetryStatsRecorderTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryStatsRecorderTask = new TelemetryStatsRecorderTask(recorder, statsProvider, telemetryRuntimeProducer);
    }

    @Test
    public void successfulExecutionClearsValuesOnProvider() {
        telemetryStatsRecorderTask.execute();

        verify(statsProvider).clearStats();
    }

    @Test
    public void executeFetchesStatsFromProvider() {

        telemetryStatsRecorderTask.execute();

        verify(statsProvider).getTelemetryStats();
    }

    @Test
    public void executeSendsStatsToRecorder() throws HttpRecorderException {
        Stats expectedStats = new Stats();
        when(statsProvider.getTelemetryStats()).thenReturn(expectedStats);
        ArgumentCaptor<Stats> captor = ArgumentCaptor.forClass(Stats.class);

        telemetryStatsRecorderTask.execute();

        verify(recorder).execute(captor.capture());
        assertEquals(expectedStats, captor.getValue());
    }

    @Test
    public void unsuccessfulExecutionDoesNotClearValuesOnProvider() {
        when(statsProvider.getTelemetryStats()).thenAnswer(invocation -> {
            throw new HttpRecorderException("test exception", "");
        });

        telemetryStatsRecorderTask.execute();

        verify(statsProvider, times(0)).clearStats();
    }

    @Test
    public void httpExceptionAddsHttpStatusToResult() throws HttpRecorderException {

        doThrow(new HttpRecorderException("", "", 500)).when(recorder).execute(any());

        SplitTaskExecutionInfo result = telemetryStatsRecorderTask.execute();

        Assert.assertEquals(500, result.getIntegerValue("HTTP_STATUS").intValue());
        Assert.assertEquals(SplitTaskType.TELEMETRY_STATS_TASK, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void latencyIsRecordedInTelemetry() {
        telemetryStatsRecorderTask.execute();

        verify(telemetryRuntimeProducer).recordSyncLatency(eq(OperationType.TELEMETRY), anyLong());
    }
}
