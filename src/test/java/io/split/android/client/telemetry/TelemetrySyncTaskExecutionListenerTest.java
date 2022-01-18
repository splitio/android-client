package io.split.android.client.telemetry;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class TelemetrySyncTaskExecutionListenerTest {

    @Mock
    private TelemetryRuntimeProducer runtimeProducer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void syncIsRecordedWhenTaskIsSuccessful() {
        TelemetrySyncTaskExecutionListener telemetrySyncTaskExecutionListener = new TelemetrySyncTaskExecutionListener(runtimeProducer, SplitTaskType.GENERIC_TASK, OperationType.SPLITS);

        telemetrySyncTaskExecutionListener.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));

        verify(runtimeProducer).recordSuccessfulSync(eq(OperationType.SPLITS), longThat(argument -> argument > 0));
    }
}
