package io.split.android.client.validators;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.split.android.client.tracker.TrafficTypeValidator;
import io.split.android.client.tracker.TrackerValidationError;

public class EventValidatorTest {

    private EventValidatorImpl validator;

    @Before
    public void setUp() {

        TrafficTypeValidator trafficTypeValidator = mock(TrafficTypeValidator.class);

        when(trafficTypeValidator.isValid("traffic1")).thenReturn(true);
        when(trafficTypeValidator.isValid("trafficType1")).thenReturn(true);
        when(trafficTypeValidator.isValid("custom")).thenReturn(true);

        validator = new EventValidatorImpl(new KeyValidatorImpl(), trafficTypeValidator);
    }

    @Test
    public void testValidEventAllValues() {
        TrackerValidationError error = validator.validate("pepe", "traffic1", "type1", 1.0, null, true);
        Assert.assertNull(error);
    }

    @Test
    public void testValidEventNullValue() {
        TrackerValidationError error = validator.validate("pepe", "traffic1", "type1", null, null, true);
        Assert.assertNull(error);
    }

    @Test
    public void testNullKey() {
        TrackerValidationError error = validator.validate(null, "traffic1", "type1", null, null, true);
        Assert.assertNotNull(error);
        Assert.assertTrue(error.isError());
        Assert.assertEquals("you passed a null key, matching key must be a non-empty string", error.getMessage());
    }

    @Test
    public void testEmptyKey() {
        TrackerValidationError error = validator.validate("", "traffic1", "type1", null, null, true);
        Assert.assertNotNull(error);
        Assert.assertTrue(error.isError());
        Assert.assertEquals("you passed an empty string, matching key must be a non-empty string", error.getMessage());
    }

    @Test
    public void testAllSpacesInKey() {
        TrackerValidationError error = validator.validate("   ", "traffic1", "type1", null, null, true);
        Assert.assertNotNull(error);
        Assert.assertTrue(error.isError());
        Assert.assertEquals("you passed an empty string, matching key must be a non-empty string", error.getMessage());
    }

    @Test
    public void testLongKey() {
        TrackerValidationError error = validator.validate(repeat("p", 300), "traffic1", "type1", null, null, true);
        Assert.assertNotNull(error);
        Assert.assertTrue(error.isError());
        Assert.assertEquals("matching key too long - must be " + ValidationConfig.getInstance().getMaximumKeyLength() + " characters or less", error.getMessage());
    }

    @Test
    public void testNullType() {
        TrackerValidationError error = validator.validate("key1", "traffic1", null, null, null, true);
        Assert.assertNotNull(error);
        Assert.assertTrue(error.isError());
        Assert.assertEquals("you passed a null or undefined event_type, event_type must be a non-empty String", error.getMessage());
    }

    @Test
    public void testEmptyType() {
        TrackerValidationError error = validator.validate("key1", "traffic1", "", null, null, true);
        Assert.assertNotNull(error);
        Assert.assertTrue(error.isError());
        Assert.assertEquals("you passed an empty event_type, event_type must be a non-empty String", error.getMessage());
    }

    @Test
    public void testAllSpacesInType() {
        TrackerValidationError error = validator.validate("key1", "traffic1", "   ", null, null, true);
        Assert.assertNotNull(error);
        Assert.assertTrue(error.isError());
        Assert.assertEquals("you passed an empty event_type, event_type must be a non-empty String", error.getMessage());
    }

    @Test
    public void testTypeName() {
        EventTypeNameHelper nameHelper = new EventTypeNameHelper();

        TrackerValidationError error1 = validator.validate("key1", "traffic1", nameHelper.getValidAllValidChars(), null, null, true);
        TrackerValidationError error2 = validator.validate("key1", "traffic1", nameHelper.getValidStartNumber(), null, null, true);
        TrackerValidationError error3 = validator.validate("key1", "traffic1", nameHelper.getInvalidChars(), null, null, true);
        TrackerValidationError error4 = validator.validate("key1", "traffic1", nameHelper.getInvalidUndercoreStart(), null, null, true);
        TrackerValidationError error5 = validator.validate("key1", "traffic1", nameHelper.getInvalidHypenStart(), null, null, true);

        Assert.assertNull(error1);
        Assert.assertNull(error2);

        Assert.assertNotNull(error3);
        Assert.assertTrue(error3.isError());
        Assert.assertEquals(buildEventTypeValidationMessage(nameHelper.getInvalidChars()), error3.getMessage());

        Assert.assertNotNull(error4);
        Assert.assertTrue(error4.isError());
        Assert.assertEquals(buildEventTypeValidationMessage(nameHelper.getInvalidUndercoreStart()), error4.getMessage());

        Assert.assertNotNull(error5);
        Assert.assertTrue(error5.isError());
        Assert.assertEquals(buildEventTypeValidationMessage(nameHelper.getInvalidHypenStart()), error5.getMessage());
    }

    @Test
    public void testNullTrafficType() {
        TrackerValidationError error = validator.validate("key1", null, "type1", null, null, true);
        Assert.assertNotNull(error);
        Assert.assertTrue(error.isError());
        Assert.assertEquals("you passed a null or undefined traffic_type_name, traffic_type_name must be a non-empty string", error.getMessage());
    }

    @Test
    public void testEmptyTrafficType() {
        TrackerValidationError error = validator.validate("key1", "", "type1", null, null, true);
        Assert.assertNotNull(error);
        Assert.assertTrue(error.isError());
        Assert.assertEquals("you passed an empty traffic_type_name, traffic_type_name must be a non-empty string", error.getMessage());
    }

    @Test
    public void testAllSpacesInTrafficType() {
        TrackerValidationError error = validator.validate("key1", "   ", "type1", null, null, true);
        Assert.assertNotNull(error);
        Assert.assertTrue(error.isError());
        Assert.assertEquals("you passed an empty traffic_type_name, traffic_type_name must be a non-empty string", error.getMessage());
    }

    @Test
    public void testUppercaseCharsInTrafficType() {
        final String uppercaseMessage = "traffic_type_name should be all lowercase - converting string to lowercase";

        TrackerValidationError error0 = validator.validate("key1", "custom", "type1", null, null, true);
        TrackerValidationError error1 = validator.validate("key1", "Custom", "type1", null, null, true);
        TrackerValidationError error2 = validator.validate("key1", "cUSTom", "type1", null, null, true);
        TrackerValidationError error3 = validator.validate("key1", "custoM", "type1", null, null, true);

        Assert.assertNull(error0);

        Assert.assertNotNull(error1);
        Assert.assertFalse(error1.isError());
        Assert.assertTrue(error1.getWarnings().contains(uppercaseMessage));

        Assert.assertNotNull(error2);
        Assert.assertFalse(error2.isError());
        Assert.assertTrue(error2.getWarnings().contains(uppercaseMessage));

        Assert.assertNotNull(error3);
        Assert.assertFalse(error3.isError());
        Assert.assertTrue(error3.getWarnings().contains(uppercaseMessage));
    }

    @Test
    public void noChachedServerTrafficType() {
        TrackerValidationError error = validator.validate("key1", "nocached", "type1", null, null, true);
        Assert.assertNotNull(error);
        Assert.assertFalse(error.isError());
        Assert.assertEquals(1, error.getWarnings().size());
        String actualWarning = error.getWarnings().get(0);
        Assert.assertTrue("Expected warning to contain 'Traffic Type nocached'",
                actualWarning.contains("Traffic Type nocached"));
        Assert.assertTrue("Expected warning to contain 'does not have any corresponding feature flags'",
                actualWarning.contains("does not have any corresponding feature flags"));
    }

    private String buildEventTypeValidationMessage(String eventType) {
        return "you passed " + eventType
                + ", event name must adhere to the regular expression " + ValidationConfig.getInstance().getTrackEventNamePattern()
                + ". This means an event name must be alphanumeric, cannot be more than 80 characters long, and can only include a dash, "
                + " underscore, period, or colon as separators of alphanumeric characters.";
    }

    private String repeat(String str, int count) {
        StringBuilder builder = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            builder.append(str);
        }
        return builder.toString();
    }
}
