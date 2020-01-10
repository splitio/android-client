package io.split.android.client.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.events.EventsRecorderTaskConfig;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.storage.events.PersistentEventsStorage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventsRecorderTaskTest {

    final static String TASK_ID = "e8fcec48-fb83-4a15-a9b6-30572f07e0e7";
    final static int DEFAULT_POP_CONFIG = 100;

    HttpRecorder<List<Event>> mEventsRecorder;
    PersistentEventsStorage mPersistentEventsStorage;
    SplitTaskExecutionListener mTaskExecutionListener;


    List<Event> mDefaultParams = new ArrayList<>();
    EventsRecorderTaskConfig mDefaultConfig = new EventsRecorderTaskConfig(DEFAULT_POP_CONFIG);

    @Before
    public void setup() {
        mDefaultParams = createEvents();
        mEventsRecorder = (HttpRecorder<List<Event>>) Mockito.mock(HttpRecorder.class);
        mPersistentEventsStorage = Mockito.mock(PersistentEventsStorage.class);
        mTaskExecutionListener = Mockito.mock(SplitTaskExecutionListener.class);
    }

    @Test
    public void correctExecution() throws HttpRecorderException {

        ArgumentCaptor<SplitTaskExecutionInfo> taskInfoCaptor = ArgumentCaptor.forClass(SplitTaskExecutionInfo.class);

        when(mPersistentEventsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());

        EventsRecorderTask task = new EventsRecorderTask(
                TASK_ID,
                mTaskExecutionListener,
                mEventsRecorder,
                mPersistentEventsStorage,
                mDefaultConfig);

        task.execute();

        verify(mEventsRecorder, times(2)).execute(mDefaultParams);
        verify(mPersistentEventsStorage, times(3)).pop(DEFAULT_POP_CONFIG);
        verify(mTaskExecutionListener, times(1)).taskExecuted(taskInfoCaptor.capture());

        SplitTaskExecutionInfo result = taskInfoCaptor.getValue();
        Assert.assertEquals(TASK_ID, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void throwingException() throws HttpRecorderException {

        ArgumentCaptor<SplitTaskExecutionInfo> taskInfoCaptor = ArgumentCaptor.forClass(SplitTaskExecutionInfo.class);

        when(mPersistentEventsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(mDefaultParams)
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("","")).when(mEventsRecorder).execute(mDefaultParams);

        EventsRecorderTask task = new EventsRecorderTask(
                TASK_ID,
                mTaskExecutionListener,
                mEventsRecorder,
                mPersistentEventsStorage,
                mDefaultConfig);

        task.execute();

        verify(mEventsRecorder, times(1)).execute(mDefaultParams);
        verify(mPersistentEventsStorage, times(2)).pop(DEFAULT_POP_CONFIG);
        verify(mTaskExecutionListener, times(1)).taskExecuted(taskInfoCaptor.capture());

        SplitTaskExecutionInfo result = taskInfoCaptor.getValue();
        Assert.assertEquals(TASK_ID, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void emptyEvents() throws HttpRecorderException {

        ArgumentCaptor<SplitTaskExecutionInfo> taskInfoCaptor = ArgumentCaptor.forClass(SplitTaskExecutionInfo.class);

        when(mPersistentEventsStorage.pop(DEFAULT_POP_CONFIG))
                .thenReturn(new ArrayList<>());
        doThrow(new HttpRecorderException("","")).when(mEventsRecorder).execute(mDefaultParams);

        EventsRecorderTask task = new EventsRecorderTask(
                TASK_ID,
                mTaskExecutionListener,
                mEventsRecorder,
                mPersistentEventsStorage,
                mDefaultConfig);

        task.execute();

        verify(mEventsRecorder, times(0)).execute(mDefaultParams);
        verify(mPersistentEventsStorage, times(1)).pop(DEFAULT_POP_CONFIG);
        verify(mTaskExecutionListener, times(1)).taskExecuted(taskInfoCaptor.capture());

        SplitTaskExecutionInfo result = taskInfoCaptor.getValue();
        Assert.assertEquals(TASK_ID, result.getTaskType());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @After
    public void tearDown() {
        reset(mEventsRecorder);
        reset(mPersistentEventsStorage);
    }

    private List<Event> createEvents() {
        List<Event> events = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            Event event = new Event();
            event.eventTypeId = "event_" + i;
            event.trafficTypeName = "custom";
            event.key = "key1";
            events.add(event);
        }
        return events;
    }
}
