package io.split.android.client.events;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.TestingHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.executors.SplitEventExecutorResources;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class EventsManagerTest {

    @Mock
    SplitEventExecutorResources resources;

    @Mock
    SplitClient client;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(resources.getSplitClient()).thenReturn(client);
    }

    @Test
    public void eventOnReady() {

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(cfg);

        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        eventManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);


        boolean shouldStop = false;
        long maxExecutionTime = System.currentTimeMillis() + 10000;
        long intervalExecutionTime = 1000;

        while(!shouldStop) {
            try {
                Thread.currentThread().sleep(intervalExecutionTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
            }

            maxExecutionTime -= intervalExecutionTime;

            if (System.currentTimeMillis() > maxExecutionTime) {
                shouldStop = true;
            }

            if (eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY)) {
                shouldStop = true;
            }
        }

        assertThat(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY), is(equalTo(true)));
        assertThat(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT), is(equalTo(false)));
    }

    @Test
    public void eventOnReadyTimedOut() {
        SplitClientConfig cfg = SplitClientConfig.builder().ready(1000).build();
        SplitEventsManager eventManager = new SplitEventsManager(cfg);

        boolean shouldStop = false;
        long maxExecutionTime = System.currentTimeMillis() + 10000;
        long intervalExecutionTime = 1000;

        while(!shouldStop) {
            try {
                Thread.currentThread().sleep(intervalExecutionTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
            }

            maxExecutionTime -= intervalExecutionTime;

            if (System.currentTimeMillis() > maxExecutionTime) {
                shouldStop = true;
            }

            if (eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT)) {
                shouldStop = true;
            }
        }

        assertThat(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY), is(equalTo(false)));
        assertThat(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT), is(equalTo(true)));
    }

    @Test
    public void eventOnReadyAndOnReadyTimedOut() {
        SplitClientConfig cfg = SplitClientConfig.builder().ready(1000).build();
        SplitEventsManager eventManager = new SplitEventsManager(cfg);

        boolean shouldStop = false;
        long maxExecutionTime = System.currentTimeMillis() + 10000;
        long intervalExecutionTime = 1000;

        while(!shouldStop) {
            try {
                Thread.currentThread().sleep(intervalExecutionTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
            }

            maxExecutionTime -= intervalExecutionTime;

            if (System.currentTimeMillis() > maxExecutionTime) {
                shouldStop = true;
            }

            if (eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT)) {
                shouldStop = true;
            }
        }

        //At this line timeout has been reached
        assertThat(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT), is(equalTo(true)));

        //But if after timeout event, the Splits and MySegments are ready, SDK_READY should be triggered
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        eventManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);

        shouldStop = false;
        maxExecutionTime = System.currentTimeMillis() + 10000;
        intervalExecutionTime = 1000;

        while(!shouldStop) {
            try {
                Thread.currentThread().sleep(intervalExecutionTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
            }

            maxExecutionTime -= intervalExecutionTime;

            if (System.currentTimeMillis() > maxExecutionTime) {
                shouldStop = true;
            }

            if (eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY)) {
                shouldStop = true;
            }
        }

        assertThat(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY), is(equalTo(true)));
        assertThat(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT), is(equalTo(true)));
    }

    @Test
    public void eventOnReadyFromCacheSplitsFirst() {
        List<SplitInternalEvent> eventList = new ArrayList<>();
        eventList.add(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
        eventOnReadyFromCache(eventList);
    }

    @Test
    public void eventOnReadyFromCacheMySegmentsFirst() {
        List<SplitInternalEvent> eventList = new ArrayList<>();
        eventList.add(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
        eventOnReadyFromCache(eventList);
    }

    @Test
    public void eventOnReadyFromCacheAttributesFirst() {
        List<SplitInternalEvent> eventList = new ArrayList<>();
        eventList.add(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        eventList.add(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
        eventOnReadyFromCache(eventList);
    }

    public void eventOnReadyFromCache(List<SplitInternalEvent> eventList) {

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(cfg);

        for(SplitInternalEvent event : eventList) {
            eventManager.notifyInternalEvent(event);
        }

        boolean shouldStop = false;
        long maxExecutionTime = System.currentTimeMillis() + 10000;
        long intervalExecutionTime = 1000;

        while(!shouldStop) {
            try {
                Thread.currentThread().sleep(intervalExecutionTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
            }

            maxExecutionTime -= intervalExecutionTime;

            if (System.currentTimeMillis() > maxExecutionTime) {
                shouldStop = true;
            }

            if (eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_FROM_CACHE)) {
                shouldStop = true;
            }
        }

        assertThat(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_FROM_CACHE), is(equalTo(true)));
    }
}
