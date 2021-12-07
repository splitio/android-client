package io.split.android.client.service.telemetry;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.storage.TelemetryConfigProvider;

public class TelemetryConfigRecorderTaskTest {

    @Mock
    private HttpRecorder<Config> recorder;
    @Mock
    private TelemetryConfigProvider configProvider;
    private TelemetryConfigRecorderTask telemetryConfigRecorderTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryConfigRecorderTask = new TelemetryConfigRecorderTask(recorder, configProvider);
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
}
