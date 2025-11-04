package io.split.android.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.events.EventsRecorderTaskConfig;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class EventsRecorderTaskTest {

    final static String TASK_ID = "e8fcec48-fb83-4a15-a9b6-30572f07e0e7";
    final static int DEFAULT_POP_CONFIG = 100;

    HttpRecorder<List<Event>> mEventsRecorder;
    PersistentEventsStorage mPersistentEventsStorage;
    TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    List<Event> mDefaultParams = new ArrayList<>();
    EventsRecorderTaskConfig mDefaultConfig = new EventsRecorderTaskConfig(DEFAULT_POP_CONFIG);

    @Before
    public void setup() {
        mDefaultParams = createEvents();
        mEventsRecorder = (HttpRecorder<List<Event>>) Mockito.mock(HttpRecorder.class);
        mPersistentEventsStorage = Mockito.mock(PersistentEventsStorage.class);
        mTelemetryRuntimeProducer = Mockito.mock(TelemetryRuntimeProducer.class);
    }

    @Test
    public void correctExecution() throws HttpRecorderException {

        when(mPersistentEventsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());

        EventsRecorderTask task = new EventsRecorderTask(
                mEventsRecorder,
                mPersistentEventsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        verify(mEventsRecorder, times(2)).execute(mDefaultParams);
        verify(mPersistentEventsStorage, times(3)).pop(DEFAULT_POP_CONFIG);
        verify(mPersistentEventsStorage, times(2)).delete(any());
        verify(mPersistentEventsStorage, never()).setActive(any());

        Assert.assertEquals(SplitTaskType.EVENTS_RECORDER, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertNull(result.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS));
        Assert.assertNull(result.getLongValue(SplitTaskExecutionInfo.NON_SENT_BYTES));
    }

    @Test
    public void throwingException() throws HttpRecorderException {

        when(mPersistentEventsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "")).when(mEventsRecorder).execute(mDefaultParams);

        EventsRecorderTask task = new EventsRecorderTask(
                mEventsRecorder,
                mPersistentEventsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        verify(mEventsRecorder, times(1)).execute(mDefaultParams);
        verify(mPersistentEventsStorage, times(2)).pop(DEFAULT_POP_CONFIG);
        verify(mPersistentEventsStorage, never()).delete(any());
        int setActiveTimes = DEFAULT_POP_CONFIG / EventsRecorderTask.FAILING_CHUNK_SIZE;
        verify(mPersistentEventsStorage, times(setActiveTimes)).setActive(any());

        Assert.assertEquals(SplitTaskType.EVENTS_RECORDER, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        Assert.assertEquals(100, result.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS).intValue());
        Assert.assertEquals(1000, result.getLongValue(SplitTaskExecutionInfo.NON_SENT_BYTES).longValue());
    }

    @Test
    public void emptyEvents() throws HttpRecorderException {

        when(mPersistentEventsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "")).when(mEventsRecorder).execute(mDefaultParams);

        EventsRecorderTask task = new EventsRecorderTask(
                mEventsRecorder,
                mPersistentEventsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        verify(mEventsRecorder, times(0)).execute(mDefaultParams);
        verify(mPersistentEventsStorage, times(1)).pop(DEFAULT_POP_CONFIG);
        verify(mPersistentEventsStorage, never()).setActive(any());

        Assert.assertEquals(SplitTaskType.EVENTS_RECORDER, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertNull(result.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS));
        Assert.assertNull(result.getLongValue(SplitTaskExecutionInfo.NON_SENT_BYTES));
    }

    @Test
    public void recordErrorInTelemetry() throws HttpRecorderException {

        when(mPersistentEventsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "", 500)).when(mEventsRecorder).execute(mDefaultParams);

        EventsRecorderTask task = new EventsRecorderTask(
                mEventsRecorder,
                mPersistentEventsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        task.execute();

        verify(mTelemetryRuntimeProducer).recordSyncError(OperationType.EVENTS, 500);
    }

    @Test
    public void recordLatencyInTelemetry() {

        when(mPersistentEventsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());

        EventsRecorderTask task = new EventsRecorderTask(
                mEventsRecorder,
                mPersistentEventsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        task.execute();

        verify(mTelemetryRuntimeProducer, atLeastOnce()).recordSyncLatency(eq(OperationType.EVENTS), anyLong());
    }

    @Test
    public void recordSuccessInTelemetry() throws HttpRecorderException {

        when(mPersistentEventsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());

        EventsRecorderTask task = new EventsRecorderTask(
                mEventsRecorder,
                mPersistentEventsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        task.execute();

        verify(mTelemetryRuntimeProducer, atLeastOnce()).recordSuccessfulSync(eq(OperationType.EVENTS), longThat(arg -> arg > 0));
    }

    @Test
    public void statusCode9009InHttpExceptionReturnsDoNotRetry() throws HttpRecorderException {
        when(mPersistentEventsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "", 9009)).when(mEventsRecorder).execute(mDefaultParams);

        EventsRecorderTask task = new EventsRecorderTask(
                mEventsRecorder,
                mPersistentEventsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        Assert.assertEquals(SplitTaskType.EVENTS_RECORDER, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        Assert.assertEquals(100, result.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS).intValue());
        Assert.assertEquals(true, result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void nullStatusCodeInExceptionReturnsNullDoNotRetry() throws HttpRecorderException {
        when(mPersistentEventsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("", "")).when(mEventsRecorder).execute(mDefaultParams);

        EventsRecorderTask task = new EventsRecorderTask(
                mEventsRecorder,
                mPersistentEventsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        SplitTaskExecutionInfo result = task.execute();

        Assert.assertEquals(SplitTaskType.EVENTS_RECORDER, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        Assert.assertEquals(100, result.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS).intValue());
        Assert.assertNull(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void statusCode9009InHttpExceptionBreaksLoop() throws HttpRecorderException {
        when(mPersistentEventsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(mDefaultParams);
        doThrow(new HttpRecorderException("", "", 9009)).when(mEventsRecorder).execute(mDefaultParams);

        EventsRecorderTask task = new EventsRecorderTask(
                mEventsRecorder,
                mPersistentEventsStorage,
                mDefaultConfig,
                mTelemetryRuntimeProducer);

        task.execute();

        verify(mPersistentEventsStorage, times(1)).pop(DEFAULT_POP_CONFIG);
        verify(mPersistentEventsStorage, times(0)).delete(any());
        verify(mPersistentEventsStorage, times(DEFAULT_POP_CONFIG / EventsRecorderTask.FAILING_CHUNK_SIZE)).setActive(any());
    }

    private List<Event> createEvents() {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Event event = new Event();
            event.eventTypeId = "event_" + i;
            event.trafficTypeName = "custom";
            event.key = "key1";
            event.setSizeInBytes(10);
            events.add(event);
        }
        return events;
    }
}
