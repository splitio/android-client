package io.split.android.client.service.telemetry;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryConfigProvider;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class TelemetryConfigRecorderTaskTest {

    @Mock
    private HttpRecorder<Config> recorder;
    @Mock
    private TelemetryConfigProvider configProvider;
    @Mock
    private TelemetryRuntimeProducer telemetryRuntimeProducer;
    private TelemetryConfigRecorderTask telemetryConfigRecorderTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryConfigRecorderTask = new TelemetryConfigRecorderTask(recorder, configProvider, telemetryRuntimeProducer);
    }

    @Test
    public void executeCallsExecuteOnRecorder() throws HttpRecorderException {
        telemetryConfigRecorderTask.execute();

        verify(recorder).execute(any());
    }

    @Test
    public void executeFetchesConfigForRecorderFromConfigProvider() {

        telemetryConfigRecorderTask.execute();

        verify(configProvider).getConfigTelemetry();
    }

    @Test
    public void executeSendsConfigFromProviderToRecorder() throws HttpRecorderException {
        Config mockConfigTelemetry = new Config();
        mockConfigTelemetry.setImpressionsListenerEnabled(true);
        mockConfigTelemetry.setTags(Arrays.asList("tag1", "tag2"));

        ArgumentCaptor<Config> argumentCaptor = ArgumentCaptor.forClass(Config.class);

        when(configProvider.getConfigTelemetry()).thenReturn(mockConfigTelemetry);

        telemetryConfigRecorderTask.execute();

        verify(recorder).execute(argumentCaptor.capture());
        assertEquals(mockConfigTelemetry.isImpressionsListenerEnabled(), argumentCaptor.getValue().isImpressionsListenerEnabled());
        assertEquals(mockConfigTelemetry.getTags(), argumentCaptor.getValue().getTags());
    }

    @Test
    public void errorIsTrackedInTelemetry() throws HttpRecorderException {

        doThrow(new HttpRecorderException("", "", 500)).when(recorder).execute(any());

        telemetryConfigRecorderTask.execute();

        verify(telemetryRuntimeProducer).recordSyncError(OperationType.TELEMETRY, 500);
    }

    @Test
    public void latencyIsTrackedInTelemetry() {
        telemetryConfigRecorderTask.execute();

        verify(telemetryRuntimeProducer).recordSyncLatency(eq(OperationType.TELEMETRY), anyLong());
    }

    @Test
    public void successIsTrackedInTelemetry() {

        telemetryConfigRecorderTask.execute();

        verify(telemetryRuntimeProducer).recordSuccessfulSync(eq(OperationType.TELEMETRY), longThat(arg -> arg > 0));
    }

    @Test
    public void status9009InHttpExceptionReturnsDoNotRetry() throws HttpRecorderException {
        doThrow(new HttpRecorderException("", "", 9009)).when(recorder).execute(any());

        SplitTaskExecutionInfo result = telemetryConfigRecorderTask.execute();

        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        Assert.assertEquals(true, result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
        Assert.assertEquals(SplitTaskType.TELEMETRY_CONFIG_TASK, result.getTaskType());
    }

    @Test
    public void nullStatusInHttpExceptionReturnsNullDoNotRetry() throws HttpRecorderException {
        doThrow(new HttpRecorderException("", "", null)).when(recorder).execute(any());

        SplitTaskExecutionInfo result = telemetryConfigRecorderTask.execute();

        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        Assert.assertNull(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
        Assert.assertEquals(SplitTaskType.TELEMETRY_CONFIG_TASK, result.getTaskType());
    }
}
