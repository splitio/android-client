package io.split.android.client.validators;

import com.google.common.base.Strings;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.split.android.client.dtos.Event;

public class EventValidatorTest {

    private EventValidator validator;

    @Before
    public void setUp() {
        validator = new EventValidatorImpl("KeyValidatorTests");
        validator.setMessageLogger(Mockito.mock(ValidationMessageLogger.class));
    }

    @Test
    public void testValidEventAllValues() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = "pepe";
        event.value = 1.0;
        Assert.assertTrue(validator.isValidEvent(event));
    }

    @Test
    public void testValidEventNullValue() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = "pepe";
        Assert.assertTrue(validator.isValidEvent(event));
    }

    @Test
    public void testNullKey() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = null;
        Assert.assertFalse(validator.isValidEvent(event));
    }

    @Test
    public void testEmptyKey() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = "";
        Assert.assertFalse(validator.isValidEvent(event));
    }

    @Test
    public void testLongKey() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = Strings.repeat("p", 300);
        Assert.assertFalse(validator.isValidEvent(event));
    }

    @Test
    public void testNullType() {
        Event event = new Event();
        event.eventTypeId = null;
        event.trafficTypeName = "traffic1";
        event.key = "key1";
        Assert.assertFalse(validator.isValidEvent(event));
    }

    @Test
    public void testEmptyType() {
        Event event = new Event();
        event.eventTypeId = "";
        event.trafficTypeName = "traffic1";
        event.key = "key1";
        Assert.assertFalse(validator.isValidEvent(event));
    }

    @Test
    public void testTypeName() {

        EventTypeNameHelper nameHelper = new EventTypeNameHelper();
        Event event1 = newEventTypeName();
        Event event2 = newEventTypeName();
        Event event3 = newEventTypeName();
        Event event4 = newEventTypeName();
        Event event5 = newEventTypeName();

        event1.eventTypeId = nameHelper.getValidAllValidChars();
        event2.eventTypeId = nameHelper.getValidStartNumber();
        event3.eventTypeId = nameHelper.getInvalidChars();
        event4.eventTypeId = nameHelper.getInvalidUndercoreStart();
        event5.eventTypeId = nameHelper.getInvalidHypenStart();

        Assert.assertTrue(validator.isValidEvent(event1));
        Assert.assertTrue(validator.isValidEvent(event2));
        Assert.assertFalse(validator.isValidEvent(event3));
        Assert.assertFalse(validator.isValidEvent(event4));
        Assert.assertFalse(validator.isValidEvent(event5));
    }

    @Test
    public void testNullTrafficType() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = null;
        event.key = "key1";
        Assert.assertFalse(validator.isValidEvent(event));
    }

    @Test
    public void testEmptyTrafficType() {
        
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "";
        event.key = "key1";
        Assert.assertFalse(validator.isValidEvent(event));
    }

    @Test
    public void testUppercaseCharsInTrafficType() {

        Event event0 = newEventUppercase();
        Event event1 = newEventUppercase();
        Event event2 = newEventUppercase();
        Event event3 = newEventUppercase();

        event0.trafficTypeName = "custom";
        event1.trafficTypeName = "Custom";
        event2.trafficTypeName = "cUSTom";
        event3.trafficTypeName = "custoM";

        Assert.assertTrue(validator.isValidEvent(event0));
        Assert.assertFalse(validator.trafficTypeHasUppercaseLetters(event0));

        Assert.assertTrue(validator.isValidEvent(event1));
        Assert.assertTrue(validator.trafficTypeHasUppercaseLetters(event1));

        Assert.assertTrue(validator.isValidEvent(event2));
        Assert.assertTrue(validator.trafficTypeHasUppercaseLetters(event2));

        Assert.assertTrue(validator.isValidEvent(event3));
        Assert.assertTrue(validator.trafficTypeHasUppercaseLetters(event3));

    }

    private Event newEventTypeName()  {
        Event event = new Event();
        event.trafficTypeName = "traffic1";
        event.key = "key1";
        return event;
    }

    private Event newEventUppercase() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.key = "key1";
        return event;
    }
}
