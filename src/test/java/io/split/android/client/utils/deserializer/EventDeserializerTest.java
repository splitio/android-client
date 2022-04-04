package io.split.android.client.utils.deserializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import io.split.android.client.dtos.Event;

public class EventDeserializerTest {

    private EventDeserializer mDeserializer;

    @Before
    public void setUp() {
        mDeserializer = new EventDeserializer();
    }

    @Test
    public void test() {
        String source = "{\"sizeInBytes\":1024,\"eventTypeId\":\"type\",\"trafficTypeName\":\"user\",\"key\":\"CUSTOMER_ID\",\"value\":1.0,\"timestamp\":1648760271141,\"properties\":{\"decimal2\":20005579852.255556,\"string\":\"plain_string\",\"null\":null,\"price\":24584535,\"decimal3\":20.45,\"true\":true,\"false\":false,\"id\":158576837,\"decimal\":20.000068,\"int\":4}}";

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Event.class, mDeserializer)
                .serializeNulls()
                .create();

        Event event = gson.fromJson(source, Event.class);
        assertEquals(1024, event.getSizeInBytes());
        assertEquals(24584535, event.properties.get("price"));
        assertEquals(158576837, event.properties.get("id"));
        assertEquals(true, event.properties.get("true"));
        assertEquals(false, event.properties.get("false"));
        assertEquals(20.000068, event.properties.get("decimal"));
        assertEquals(new BigDecimal("20005579852.255556"), event.properties.get("decimal2"));
        assertEquals(20.45, event.properties.get("decimal3"));
        assertEquals("plain_string", event.properties.get("string"));
        assertEquals(4, event.properties.get("int"));
        assertNull(event.properties.get("null"));

        String finalString = gson.toJson(event);
        assertEquals(source, finalString);
    }
}
