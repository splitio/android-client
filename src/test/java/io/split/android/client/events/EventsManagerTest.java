package io.split.android.client.events;

import org.junit.Assert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.SplitClientConfig;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EventsManagerTest {

    @Test
    public void eventOnReady() {

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(cfg);

        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_ARE_READY);
        eventManager.notifyInternalEvent(SplitInternalEvent.MYSEGEMENTS_ARE_READY);


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
        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_ARE_READY);
        eventManager.notifyInternalEvent(SplitInternalEvent.MYSEGEMENTS_ARE_READY);

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
        eventList.add(SplitInternalEvent.MYSEGMENTS_LOADED_FROM_STORAGE);
        eventOnReadyFromCache(eventList);
    }

    @Test
    public void eventOnReadyFromCacheMySegmentsFirst() {
        List<SplitInternalEvent> eventList = new ArrayList<>();
        eventList.add(SplitInternalEvent.MYSEGMENTS_LOADED_FROM_STORAGE);
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
