package io.split.android.client.events;

import junit.framework.Assert;

import org.junit.Test;

import io.split.android.client.SplitClientConfig;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EventsManagerTest {

    @Test
    public void event_on_ready() {

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
    public void event_on_ready_timed_out() {
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
    public void event_on_ready_and_on_ready_timed_out() {
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
}
