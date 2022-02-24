package io.split.android.client.service.events;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Event;

public class EventsRequestBodySerializerTest {

    private EventsRequestBodySerializer mSerializer;

    @Before
    public void setUp() {
       mSerializer = new EventsRequestBodySerializer();
    }

    @Test
    public void serialize() {
        String expectedJson = "[{\"eventTypeId\":\"test\",\"trafficTypeName\":\"default\",\"key\":\"user_key\",\"value\":0.0,\"timestamp\":1645461035249,\"properties\":{\"test_prop_1\":\"test\",\"test_prop_2\":100.0,\"test_prop_3\":50.05}},{\"eventTypeId\":\"test\",\"trafficTypeName\":\"default\",\"key\":\"user_key\",\"value\":0.0,\"timestamp\":1645461036024,\"properties\":{\"test_prop_1\":\"test\",\"test_prop_2\":100.0,\"test_prop_3\":50.05}}]";
        Event event1 = new Event();
        event1.eventTypeId = "test";
        event1.key = "user_key";
        Map<String, Object> propertiesMap = new HashMap<>();
        propertiesMap.put("test_prop_1", "test");
        propertiesMap.put("test_prop_2", 100.0);
        propertiesMap.put("test_prop_3", 50.05);
        event1.properties = propertiesMap;
        event1.timestamp = 1645461035249L;
        event1.trafficTypeName = "default";
        event1.value = 0.0;

        Event event2 = new Event();
        event2.eventTypeId = "test";
        event2.key = "user_key";
        event2.properties = propertiesMap;
        event2.timestamp = 1645461036024L;
        event2.trafficTypeName = "default";
        event2.value = 0.0;

        List<Event> events = Arrays.asList(event1, event2);

        assertEquals(expectedJson, mSerializer.serialize(events));
    }
}
