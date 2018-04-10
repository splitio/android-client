package io.split.android.client.events;

import org.junit.Test;

import io.split.android.client.SplitClientConfig;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EventsManagerTest {

    @Test
    public void event_on_ready(){

        SplitClientConfig cfg = SplitClientConfig.builder().build();
        SplitEventsManager eventManager = new SplitEventsManager(cfg);

        eventManager.notifyInternalEvent(SplitInternalEvent.SPLITS_ARE_READY);
        eventManager.notifyInternalEvent(SplitInternalEvent.MYSEGEMENTS_ARE_READY);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY), is(equalTo(true)));
        assertThat(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT), is(equalTo(false)));
    }

    @Test
    public void event_on_ready_timed_out(){
        SplitClientConfig cfg = SplitClientConfig.builder().ready(1000).build();
        SplitEventsManager eventManager = new SplitEventsManager(cfg);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY), is(equalTo(false)));
        assertThat(eventManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT), is(equalTo(true)));
    }
}
