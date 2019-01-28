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
        validator = new EventValidator("KeyValidatorTests");
        validator.setMessageLogger(Mockito.mock(ValidationMessageLogger.class));
    }

    @Test
    public void testValidEventAllValues() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = "pepe";
        event.value = 1.0;
        Assert.assertTrue(event.isValid(validator));
        Assert.assertEquals(EventValidator.NO_ERROR, validator.getError());
        Assert.assertEquals(0, validator.getWarnings().size());
    }

    @Test
    public void testValidEventNullValue() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = "pepe";
        Assert.assertTrue(event.isValid(validator));
        Assert.assertEquals(EventValidator.NO_ERROR, validator.getError());
        Assert.assertEquals(0, validator.getWarnings().size());
    }

    @Test
    public void testNullKey() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = null;
        Assert.assertFalse(event.isValid(validator));
        Assert.assertFalse(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(EventValidator.ERROR_NULL_KEY, validator.getError());
        Assert.assertEquals(0, validator.getWarnings().size());
    }

    @Test
    public void testEmptyKey() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = "";
        Assert.assertFalse(event.isValid(validator));
        Assert.assertFalse(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(validator.getError(), EventValidator.ERROR_EMPTY_KEY);
        Assert.assertEquals(0, validator.getWarnings().size());
    }

    @Test
    public void testLongKey() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = Strings.repeat("p", 300);
        Assert.assertFalse(event.isValid(validator));
        Assert.assertFalse(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(EventValidator.ERROR_LONG_KEY, validator.getError());
        Assert.assertEquals(0, validator.getWarnings().size());
    }

    @Test
    public void testNullType() {
        Event event = new Event();
        event.eventTypeId = null;
        event.trafficTypeName = "traffic1";
        event.key = "key1";
        Assert.assertFalse(event.isValid(validator));
        Assert.assertFalse(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(EventValidator.ERROR_NULL_TYPE, validator.getError());
        Assert.assertEquals(0, validator.getWarnings().size());
    }

    @Test
    public void testEmptyType() {
        Event event = new Event();
        event.eventTypeId = "";
        event.trafficTypeName = "traffic1";
        event.key = "key1";
        Assert.assertFalse(event.isValid(validator));
        Assert.assertFalse(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(EventValidator.ERROR_EMPTY_TYPE, validator.getError());
        Assert.assertEquals(0, validator.getWarnings().size());

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

        Assert.assertTrue(event1.isValid(validator));
        Assert.assertTrue(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(0, validator.getWarnings().size());

        Assert.assertTrue(event2.isValid(validator));
        Assert.assertTrue(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(0, validator.getWarnings().size());

        Assert.assertFalse(event3.isValid(validator));
        Assert.assertFalse(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(EventValidator.ERROR_REGEX_TYPE, validator.getError());
        Assert.assertEquals(0, validator.getWarnings().size());

        Assert.assertFalse(event4.isValid(validator));
        Assert.assertFalse(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(EventValidator.ERROR_REGEX_TYPE, validator.getError());
        Assert.assertEquals(0, validator.getWarnings().size());

        Assert.assertFalse(event5.isValid(validator));
        Assert.assertFalse(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(EventValidator.ERROR_REGEX_TYPE, validator.getError());
        Assert.assertEquals(0, validator.getWarnings().size());
    }

    @Test
    public void testNullTrafficType() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = null;
        event.key = "key1";
        Assert.assertFalse(event.isValid(validator));
        Assert.assertFalse(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(EventValidator.ERROR_NULL_TRAFFIC_TYPE, validator.getError());
        Assert.assertEquals(0, validator.getWarnings().size());
    }

    @Test
    public void testEmptyTrafficType() {
        
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "";
        event.key = "key1";
        Assert.assertFalse(event.isValid(validator));
        Assert.assertFalse(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(EventValidator.ERROR_EMPTY_TRAFFIC_TYPE, validator.getError());
        Assert.assertEquals(0, validator.getWarnings().size());
    }

    @Test
    public void testUppercaseCharsInTrafficType() {

        Event event1 = newEventUppercase();
        Event event2 = newEventUppercase();
        Event event3 = newEventUppercase();
        event1.trafficTypeName = "Custom";
        event2.trafficTypeName = "cUSTom";
        event3.trafficTypeName = "custoM";

        Assert.assertTrue(event1.isValid(validator));
        Assert.assertTrue(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(1, validator.getWarnings().size());

        if (validator.getWarnings().size() > 0) {
            Assert.assertEquals(EventValidator.WARNING_UPPERCASE_CHARS_IN_TRAFFIC_TYPE, validator.getWarnings().get(0).intValue());
        }

        Assert.assertTrue(event2.isValid(validator));
        Assert.assertTrue(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(1, validator.getWarnings().size());
        if (validator.getWarnings().size()> 0) {
            Assert.assertEquals(EventValidator.WARNING_UPPERCASE_CHARS_IN_TRAFFIC_TYPE, validator.getWarnings().get(0).intValue());
        }

        Assert.assertTrue(event2.isValid(validator));
        Assert.assertTrue(validator.getError() == EventValidator.NO_ERROR);
        Assert.assertEquals(1, validator.getWarnings().size());
        if (validator.getWarnings().size() > 0) {
            Assert.assertEquals(EventValidator.WARNING_UPPERCASE_CHARS_IN_TRAFFIC_TYPE, validator.getWarnings().get(0).intValue());
        }
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
