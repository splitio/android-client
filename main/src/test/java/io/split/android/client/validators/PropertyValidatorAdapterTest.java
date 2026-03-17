package io.split.android.client.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.tracker.TrackerPropertyValidator;

public class PropertyValidatorAdapterTest {

    @Mock
    private TrackerPropertyValidator mDelegate;

    private PropertyValidatorAdapter mAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mAdapter = new PropertyValidatorAdapter(mDelegate);
    }

    @Test
    public void validateDelegatesToTrackerValidatorAndReturnsValidResult() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("key1", "value1");

        TrackerPropertyValidator.TrackerPropertyResult delegateResult =
                TrackerPropertyValidator.TrackerPropertyResult.valid(properties, 100);
        when(mDelegate.validate(eq(properties), eq(0), eq("test-tag")))
                .thenReturn(delegateResult);

        PropertyValidator.Result result = mAdapter.validate(properties, "test-tag");

        assertTrue(result.isValid());
        assertEquals(properties, result.getProperties());
        assertEquals(100, result.getSizeInBytes());
        assertNull(result.getErrorMessage());
        verify(mDelegate).validate(eq(properties), eq(0), eq("test-tag"));
    }

    @Test
    public void validateDelegatesToTrackerValidatorAndReturnsInvalidResult() {
        Map<String, Object> properties = new HashMap<>();

        TrackerPropertyValidator.TrackerPropertyResult delegateResult =
                TrackerPropertyValidator.TrackerPropertyResult.invalid("Properties are too large", 50);
        when(mDelegate.validate(eq(properties), eq(0), eq("test-tag")))
                .thenReturn(delegateResult);

        PropertyValidator.Result result = mAdapter.validate(properties, "test-tag");

        assertFalse(result.isValid());
        assertNull(result.getProperties());
        assertEquals(50, result.getSizeInBytes());
        assertEquals("Properties are too large", result.getErrorMessage());
        verify(mDelegate).validate(eq(properties), eq(0), eq("test-tag"));
    }

    @Test
    public void validatePassesZeroAsInitialSizeInBytes() {
        Map<String, Object> properties = new HashMap<>();
        TrackerPropertyValidator.TrackerPropertyResult delegateResult =
                TrackerPropertyValidator.TrackerPropertyResult.valid(properties, 0);
        when(mDelegate.validate(eq(properties), eq(0), eq("tag")))
                .thenReturn(delegateResult);

        mAdapter.validate(properties, "tag");

        verify(mDelegate).validate(eq(properties), eq(0), eq("tag"));
    }
}
