package io.split.android.client.validators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.storage.splits.SplitsStorage;

public class TrafficTypeValidatorImplTest {

    @Mock
    private SplitsStorage mSplitsStorage;

    private TrafficTypeValidatorImpl mValidator;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mValidator = new TrafficTypeValidatorImpl(mSplitsStorage);
    }

    @Test
    public void isValidDelegatesToSplitsStorage() {
        when(mSplitsStorage.isValidTrafficType("user")).thenReturn(true);

        boolean result = mValidator.isValid("user");

        assertTrue(result);
        verify(mSplitsStorage).isValidTrafficType("user");
    }

    @Test
    public void isValidReturnsFalseWhenStorageReturnsFalse() {
        when(mSplitsStorage.isValidTrafficType("unknown")).thenReturn(false);

        boolean result = mValidator.isValid("unknown");

        assertFalse(result);
        verify(mSplitsStorage).isValidTrafficType("unknown");
    }
}
