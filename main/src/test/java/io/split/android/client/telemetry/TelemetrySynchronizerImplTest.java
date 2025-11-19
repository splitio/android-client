package io.split.android.client.telemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static io.split.android.client.service.executor.SplitTaskExecutionInfo.DO_NOT_RETRY;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.telemetry.TelemetryConfigRecorderTask;
import io.split.android.client.service.telemetry.TelemetryStatsRecorderTask;
import io.split.android.client.service.telemetry.TelemetryTaskFactory;
import io.split.android.client.telemetry.model.OperationType;

public class TelemetrySynchronizerImplTest {

    @Mock
    private SplitTaskExecutor taskExecutor;
    @Mock
    private TelemetryTaskFactory taskFactory;
    @Mock
    private RetryBackoffCounterTimer configTimer;
    @Mock
    private TelemetryConfigRecorderTask telemetryConfigTask;
    @Mock
    private TelemetryStatsRecorderTask telemetryStatsRecorderTask;

    private TelemetrySynchronizerImpl telemetrySynchronizer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetrySynchronizer = new TelemetrySynchronizerImpl(taskExecutor, taskFactory, configTimer, 15L);
    }

    @Test
    public void synchronizeConfigSetsCorrectTaskInTimer() {

        when(taskFactory.getTelemetryConfigRecorderTask()).thenReturn(telemetryConfigTask);

        telemetrySynchronizer.synchronizeConfig();

        verify(configTimer).setTask(eq(telemetryConfigTask), any());
    }

    @Test
    public void synchronizeConfigCallsStartInConfigTimer() {

        telemetrySynchronizer.synchronizeConfig();

        verify(configTimer).start();
    }

    @Test
    public void synchronizeStatsSchedulesCorrectTaskInTaskExecutor() {

        when(taskFactory.getTelemetryStatsRecorderTask()).thenReturn(telemetryStatsRecorderTask);

        telemetrySynchronizer.synchronizeStats();

        verify(taskExecutor).schedule(eq(telemetryStatsRecorderTask), eq(5L), eq(15L), any());
    }

    @Test
    public void destroyStopsStatsSynchronizationStopsTaskOnTaskExecutor() {
        when(taskExecutor.schedule(
                any(),
                eq(5L),
                eq(15L),
                any()
        )).thenReturn("taskId");

        telemetrySynchronizer.synchronizeStats();

        telemetrySynchronizer.destroy();

        verify(taskExecutor).stopTask("taskId");
    }

    @Test
    public void destroyDoesNotStopStatsSynchronizationWhenTaskHasNotBeenScheduled() {

        telemetrySynchronizer.destroy();

        verifyNoMoreInteractions(taskExecutor);
    }

    @Test
    public void destroyCancelsConfigTimer() {

        telemetrySynchronizer.destroy();

        verify(configTimer).stop();
    }

    @Test
    public void flushSubmitsStatsTask() {

        when(taskFactory.getTelemetryStatsRecorderTask()).thenReturn(telemetryStatsRecorderTask);

        telemetrySynchronizer.flush();

        verify(taskExecutor).submit(eq(telemetryStatsRecorderTask), any());
    }

    @Test
    public void flushDoesNotSubmitStatsTaskWhenDoNotRetryHasBeenReceived() {
        when(taskFactory.getTelemetryStatsRecorderTask()).thenReturn(telemetryStatsRecorderTask);
        when(taskFactory.getTelemetryConfigRecorderTask()).thenReturn(telemetryConfigTask);
        when(telemetryStatsRecorderTask.execute()).thenReturn(
                SplitTaskExecutionInfo.error(
                        SplitTaskType.TELEMETRY_STATS_TASK,
                        Collections.singletonMap(DO_NOT_RETRY, true)));

        when(taskExecutor.schedule(
                any(),
                eq(5L),
                eq(15L),
                any()
        )).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                ((SplitTaskExecutionListener) invocation.getArgument(3)).taskExecuted(
                        ((TelemetryStatsRecorderTask) invocation.getArgument(0)).execute());
                return "taskId";
            }
        });

        telemetrySynchronizer = new TelemetrySynchronizerImpl(taskExecutor,
                taskFactory,
                configTimer,
                15L);

        telemetrySynchronizer.synchronizeStats();
        telemetrySynchronizer.flush();

        verify(telemetryStatsRecorderTask).execute();
        verify(telemetryStatsRecorderTask, times(1)).execute();
    }
}
