package io.split.android.client.validators;

import com.google.common.base.Strings;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import io.split.android.client.cache.ISplitCache;
import io.split.android.client.cache.SplitCache;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.FileStorage;

public class EventValidatorTest {

    private EventValidator validator;

    @Before
    public void setUp() {
        ISplitCache splitCache = new SplitCache(new FileStorage(new File("./build", "."), "folder"));
        splitCache.addSplit(newSplit("s0", "traffic1", Status.ACTIVE));
        splitCache.addSplit(newSplit("s1", "trafficType1", Status.ACTIVE));
        splitCache.addSplit(newSplit("s2", "custom", Status.ACTIVE));

        validator = new EventValidatorImpl(new KeyValidatorImpl(), splitCache);
    }

    @Test
    public void testValidEventAllValues() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = "pepe";
        event.value = 1.0;
        ValidationErrorInfo errorInfo = validator.validate(event, true);

        Assert.assertNull(errorInfo);
    }

    @Test
    public void testValidEventNullValue() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = "pepe";
        ValidationErrorInfo errorInfo = validator.validate(event, true);

        Assert.assertNull(errorInfo);
    }

    @Test
    public void testNullKey() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = null;
        ValidationErrorInfo errorInfo = validator.validate(event, true);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed a null key, matching key must be a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testEmptyKey() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = "";
        ValidationErrorInfo errorInfo = validator.validate(event, true);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed an empty string, matching key must be a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testAllSpacesInKey() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = "   ";
        ValidationErrorInfo errorInfo = validator.validate(event, true);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed an empty string, matching key must be a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testLongKey() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "traffic1";
        event.key = Strings.repeat("p", 300);
        ValidationErrorInfo errorInfo = validator.validate(event, true);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("matching key too long - must be " + ValidationConfig.getInstance().getMaximumKeyLength() + " characters or less", errorInfo.getErrorMessage());
    }

    @Test
    public void testNullType() {
        Event event = new Event();
        event.eventTypeId = null;
        event.trafficTypeName = "traffic1";
        event.key = "key1";
        ValidationErrorInfo errorInfo = validator.validate(event, true);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed a null or undefined event_type, event_type must be a non-empty String", errorInfo.getErrorMessage());
    }

    @Test
    public void testEmptyType() {
        Event event = new Event();
        event.eventTypeId = "";
        event.trafficTypeName = "traffic1";
        event.key = "key1";
        ValidationErrorInfo errorInfo = validator.validate(event, true);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed an empty event_type, event_type must be a non-empty String", errorInfo.getErrorMessage());
    }

    @Test
    public void testAllSpacesInType() {
        Event event = new Event();
        event.eventTypeId = "   ";
        event.trafficTypeName = "traffic1";
        event.key = "key1";
        ValidationErrorInfo errorInfo = validator.validate(event, true);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed an empty event_type, event_type must be a non-empty String", errorInfo.getErrorMessage());
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

        ValidationErrorInfo errorInfo1 = validator.validate(event1, true);
        ValidationErrorInfo errorInfo2 = validator.validate(event2, true);
        ValidationErrorInfo errorInfo3 = validator.validate(event3, true);
        ValidationErrorInfo errorInfo4 = validator.validate(event4, true);
        ValidationErrorInfo errorInfo5 = validator.validate(event5, true);

        Assert.assertNull(errorInfo1);

        Assert.assertNull(errorInfo2);

        Assert.assertNotNull(errorInfo3);
        Assert.assertTrue(errorInfo3.isError());
        Assert.assertEquals(buildEventTypeValidationMessage(event3.eventTypeId), errorInfo3.getErrorMessage());

        Assert.assertNotNull(errorInfo4);
        Assert.assertTrue(errorInfo4.isError());
        Assert.assertEquals(buildEventTypeValidationMessage(event4.eventTypeId), errorInfo4.getErrorMessage());

        Assert.assertNotNull(errorInfo5);
        Assert.assertTrue(errorInfo5.isError());
        Assert.assertEquals(buildEventTypeValidationMessage(event5.eventTypeId), errorInfo5.getErrorMessage());
    }

    @Test
    public void testNullTrafficType() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = null;
        event.key = "key1";
        ValidationErrorInfo errorInfo = validator.validate(event, true);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed a null or undefined traffic_type_name, traffic_type_name must be a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testEmptyTrafficType() {

        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "";
        event.key = "key1";
        ValidationErrorInfo errorInfo = validator.validate(event, true);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed an empty traffic_type_name, traffic_type_name must be a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testAllSpacesInTrafficType() {

        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "   ";
        event.key = "key1";
        ValidationErrorInfo errorInfo = validator.validate(event, true);

        Assert.assertNotNull(errorInfo);
        Assert.assertTrue(errorInfo.isError());
        Assert.assertEquals("you passed an empty traffic_type_name, traffic_type_name must be a non-empty string", errorInfo.getErrorMessage());
    }

    @Test
    public void testUppercaseCharsInTrafficType() {

        Event event0 = newEventUppercase();
        Event event1 = newEventUppercase();
        Event event2 = newEventUppercase();
        Event event3 = newEventUppercase();

        final String uppercaseMessage = "traffic_type_name should be all lowercase - converting string to lowercase";

        event0.trafficTypeName = "custom";
        event1.trafficTypeName = "Custom";
        event2.trafficTypeName = "cUSTom";
        event3.trafficTypeName = "custoM";

        ValidationErrorInfo errorInfo0 = validator.validate(event0, true);
        ValidationErrorInfo errorInfo1 = validator.validate(event1, true);
        ValidationErrorInfo errorInfo2 = validator.validate(event2, true);
        ValidationErrorInfo errorInfo3 = validator.validate(event3, true);


        Assert.assertNull(errorInfo0);

        Assert.assertNotNull(errorInfo1);
        Assert.assertFalse(errorInfo1.isError());
        Assert.assertEquals(uppercaseMessage, errorInfo1.getWarnings().get(ValidationErrorInfo.WARNING_TRAFFIC_TYPE_HAS_UPPERCASE_CHARS));

        Assert.assertNotNull(errorInfo2);
        Assert.assertFalse(errorInfo2.isError());
        Assert.assertEquals(uppercaseMessage, errorInfo2.getWarnings().get(ValidationErrorInfo.WARNING_TRAFFIC_TYPE_HAS_UPPERCASE_CHARS));

        Assert.assertNotNull(errorInfo3);
        Assert.assertFalse(errorInfo3.isError());
        Assert.assertEquals(uppercaseMessage, errorInfo3.getWarnings().get(ValidationErrorInfo.WARNING_TRAFFIC_TYPE_HAS_UPPERCASE_CHARS));
    }

    @Test
    public void noChachedServerTrafficType() {
        Event event = new Event();
        event.eventTypeId = "type1";
        event.trafficTypeName = "nocached";
        event.key = "key1";

        ValidationErrorInfo errorInfo = validator.validate(event, true);

        Assert.assertNotNull(errorInfo);
        Assert.assertFalse(errorInfo.isError());
        Assert.assertEquals("Traffic Type nocached does not have any corresponding Splits in this environment, "
                + "make sure you’re tracking your events to a valid traffic type defined in the Split console", errorInfo.getWarnings().get(ValidationErrorInfo.WARNING_TRAFFIC_TYPE_WITHOUT_SPLIT_IN_ENVIRONMENT));
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

    private String buildEventTypeValidationMessage(String eventType) {
        return "you passed " + eventType
                + ", event name must adhere to the regular expression " + ValidationConfig.getInstance().getTrackEventNamePattern()
                + ". This means an event name must be alphanumeric, cannot be more than 80 characters long, and can only include a dash, "
                + " underscore, period, or colon as separators of alphanumeric characters.";
    }

    private Split newSplit(String name, String trafficType, Status status) {
        Split split = new Split();
        split.name = name;
        split.trafficTypeName = trafficType;
        split.status = status;
        return split;
    }
}
