package io.split.android.client.service.workmanager.splits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.work.Data;
import androidx.work.WorkerParameters;

import org.junit.Test;

public class SplitsSyncWorkerParamsTest {

    @Test
    public void valuesFromWorkerParamsAreCorrectlySet() {
        WorkerParameters workerParameters = mock(WorkerParameters.class);
        when(workerParameters.getInputData()).thenReturn(
                new Data.Builder()
                        .putBoolean("shouldRecordTelemetry", true)
                        .putString("apiKey", "api_key")
                        .putBoolean("encryptionEnabled", true)
                        .putString("configuredFilterType", "configured_filter_type")
                        .putStringArray("configuredFilterValues", new String[]{"configured_filter_values"})
                        .putString("flagsSpec", "flags_spec")
                        .build());

        SplitsSyncWorkerParams splitsSyncWorkerParams = new SplitsSyncWorkerParams(workerParameters);

        assertTrue(splitsSyncWorkerParams.shouldRecordTelemetry());
        assertEquals("api_key", splitsSyncWorkerParams.apiKey());
        assertTrue(splitsSyncWorkerParams.encryptionEnabled());
        assertEquals("configured_filter_type", splitsSyncWorkerParams.configuredFilterType());
        assertEquals(1, splitsSyncWorkerParams.configuredFilterValues().length);
        assertEquals("configured_filter_values", splitsSyncWorkerParams.configuredFilterValues()[0]);
    }
}
