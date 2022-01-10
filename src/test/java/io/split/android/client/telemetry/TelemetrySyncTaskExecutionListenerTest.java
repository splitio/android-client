package io.split.android.client.telemetry;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class TelemetrySyncTaskExecutionListenerTest {

    @Mock
    private TelemetryRuntimeProducer runtimeProducer;
    private TelemetrySyncTaskExecutionListener telemetrySyncTaskExecutionListener;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void syncIsRecordedWhenTaskIsSuccessful() {
        telemetrySyncTaskExecutionListener = new TelemetrySyncTaskExecutionListener(runtimeProducer, SplitTaskType.GENERIC_TASK, OperationType.SPLITS);

        telemetrySyncTaskExecutionListener.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));

        verify(runtimeProducer).recordSuccessfulSync(eq(OperationType.SPLITS), longThat(argument -> argument > 0));
    }

    @Test
    public void syncErrorIsRecordedWhenTaskFails() {
        telemetrySyncTaskExecutionListener = new TelemetrySyncTaskExecutionListener(runtimeProducer, SplitTaskType.GENERIC_TASK, OperationType.SPLITS);

        telemetrySyncTaskExecutionListener.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK));

        verify(runtimeProducer).recordSyncError(eq(OperationType.SPLITS), intThat(new ArgumentMatcher<Integer>() {
            @Override
            public boolean matches(Integer argument) {
                return argument >= 400;
            }
        }));
    }
}
