package io.harness.events;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

public class EventsManagersTest {

    @Test
    public void createDeliversEventsManagerCore() {
        EventsManager<Boolean, Object, Void> eventsManager = EventsManagers.create(EventsManagerConfig.empty(), mock(EventDelivery.class));
        assertTrue(eventsManager instanceof EventsManagerCore);
    }
}
