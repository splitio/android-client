package io.split.android.client.track;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.dtos.Event;

public class EventBuilderTest {

    private EventBuilder builder;

    @Before
    public void setUp() {
        builder = new EventBuilder();
    }

    @Test
    public void testAllValuesFilled() {
        Event event = null;
        try {
            builder
                    .setTrafficType("custom")
                    .setMatchingKey("key1")
                    .setType("type1")
                    .setValue(1.0);
            event = builder.build();
        } catch (EventBuilder.EventValidationException ve) {
        }

        Assert.assertNotNull(event);
        Assert.assertEquals("type1", event.eventTypeId);
        Assert.assertEquals("key1", event.key);
        Assert.assertEquals("custom", event.trafficTypeName);
        Assert.assertEquals(1.0, event.value);
        Assert.assertNotNull(event.timestamp);
    }

    @Test
    public void testEventValueZero() {
        Event event = null;
        try {
            event = builder.setTrafficType("custom")
                    .setMatchingKey("key1")
                    .setType("type1")
                    .build();
        } catch (EventBuilder.EventValidationException ve) {
        }

        Assert.assertNotNull(event);
        Assert.assertEquals("type1", event.eventTypeId);
        Assert.assertEquals("key1", event.key);
        Assert.assertEquals("custom", event.trafficTypeName);
        Assert.assertEquals(0.0, event.value);
        Assert.assertNotNull(event.timestamp);
    }

    @Test
    public void testNullTrafficType() {
        boolean trafficTypeErrorOccurs = false;
        Event event = null;
        try {
            event = builder
                    .setMatchingKey("key1")
                    .setType("type1")
                    .build();
        } catch (EventBuilder.EventValidationException ve) {
            trafficTypeErrorOccurs = true;
        } catch (Exception e) {
        }

        Assert.assertNull(event);
        Assert.assertTrue(trafficTypeErrorOccurs);

    }

    @Test
    public void testNullMatchingKey() {
        boolean keyErrorOccurs = false;


        Event event = null;
        try {
            event = builder
                    .setType("type1")
                    .setTrafficType("custom")
                    .build();
        } catch (EventBuilder.EventValidationException ve) {
            keyErrorOccurs = true;
        } catch (Exception e) {
        }

        Assert.assertNull(event);
        Assert.assertTrue(keyErrorOccurs);
    }

    @Test
    public void testNullType() {
        boolean typeErrorOccurs = false;

        Event event = null;
        try {
            event = builder
                    .setMatchingKey("key1")
                    .setTrafficType("custom")
                    .build();
        } catch (EventBuilder.EventValidationException ve) {
            typeErrorOccurs = true;
        } catch (Exception e) {
        }

        Assert.assertNull(event);
        Assert.assertTrue(typeErrorOccurs);
    }

    @Test
    public void testValidTypeName() {
        final String TYPE_NAME = "Abcdefghij:klmnopkrstuvwxyz_-12345.6789:";
        Event event = null;
        try {
            event = builder
                    .setMatchingKey("key1")
                    .setTrafficType("custom")
                    .setType(TYPE_NAME)
                    .build();
        } catch (EventBuilder.EventValidationException ve) {
        }

        Assert.assertNotNull(event);
        Assert.assertEquals(TYPE_NAME, event.eventTypeId);
    }

    @Test
    public void testNumberStartValidTypeName() {
        final String TYPE_NAME = "1Abcdefghijklmnopkrstuvwxyz_-12345.6789:";
        Event event = null;
        try {
            event = builder
                    .setMatchingKey("key1")
                    .setTrafficType("custom")
                    .setType(TYPE_NAME)
                    .build();
        } catch (EventBuilder.EventValidationException ve) {
        }

        Assert.assertNotNull(event);
        Assert.assertEquals(TYPE_NAME, event.eventTypeId);
    }

    @Test
    public void testHypenStartInvalidTypeName() {
        final String TYPE_NAME = "-1Abcdefghijklmnopkrstuvwxyz_-123456789:";
        Event event = null;
        try {
            event = builder
                    .setMatchingKey("key1")
                    .setTrafficType("custom")
                    .setType(TYPE_NAME)
                    .build();
        } catch (EventBuilder.EventValidationException ve) {
        }

        Assert.assertNull(event);
    }

    @Test
    public void testUndercoreStartInvalidTypeName() {
        final String TYPE_NAME = "_1Abcdefghijklmnopkrstuvwxyz_-123456789:";
        Event event = null;
        try {
            event = builder
                    .setMatchingKey("key1")
                    .setTrafficType("custom")
                    .setType(TYPE_NAME)
                    .build();
        } catch (EventBuilder.EventValidationException ve) {
        }

        Assert.assertNull(event);
    }

    @Test
    public void testInvalidCharsTypeName() {
        final String TYPE_NAME = "Abcd,;][}{efghijklmnopkrstuvwxyz_-123456789:";
        Event event = null;
        try {
            event = builder
                    .setMatchingKey("key1")
                    .setTrafficType("custom")
                    .setType(TYPE_NAME)
                    .build();
        } catch (EventBuilder.EventValidationException ve) {
        }
        Assert.assertNull(event);
    }
}
