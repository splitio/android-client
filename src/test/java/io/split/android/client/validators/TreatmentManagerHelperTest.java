package io.split.android.client.validators;

import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import io.split.android.client.SplitResult;
import io.split.android.grammar.Treatments;

public class TreatmentManagerHelperTest {

    @Test
    public void controlTreatmentsForSplitsValidatesSplitsWhenValidatorAndLoggerAreNotNull() {
        SplitValidator validator = mock(SplitValidator.class);
        ValidationMessageLogger logger = mock(ValidationMessageLogger.class);

        when(validator.validateName("split2")).thenReturn(new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "message"));

        TreatmentManagerHelper.controlTreatmentsForSplits(Arrays.asList("split1", "split2"), "tag", validator, logger);

        verify(validator).validateName("split1");
        verify(validator).validateName("split2");
        verify(logger, atMostOnce()).e("message", "tag");
    }

    @Test
    public void controlTreatmentsForSplitsDoesNotValidateSplitsWhenValidatorOrLoggerAreNull() {

        Map<String, String> result = TreatmentManagerHelper.controlTreatmentsForSplits(Arrays.asList("split1", "split2"));

        Assert.assertEquals(2, result.size());
    }

    @Test
    public void controlTreatmentsForSplitsWithConfigValidatesSplitsWhenValidatorAndLoggerAreNotNull() {
        SplitValidator validator = mock(SplitValidator.class);
        ValidationMessageLogger logger = mock(ValidationMessageLogger.class);

        when(validator.validateName("split2")).thenReturn(new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "message"));

        TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(Arrays.asList("split1", "split2"), "tag", validator, logger);

        verify(validator).validateName("split1");
        verify(validator).validateName("split2");
        verify(logger, atMostOnce()).e("message", "tag");
    }

    @Test
    public void controlTreatmentsForSplitsWithConfigDoesNotValidateSplitsWhenValidatorOrLoggerAreNull() {

        Map<String, SplitResult> result = TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(Arrays.asList("split1", "split2"));

        Assert.assertEquals(2, result.size());
    }

    @Test
    public void controlTreatmentsForSplitsReturnsControlTreatments() {

        Map<String, String> result = TreatmentManagerHelper.controlTreatmentsForSplits(Arrays.asList("split1", "split2"));

        Assert.assertTrue(result.values().stream().allMatch(Treatments.CONTROL::equals));
    }

    @Test
    public void controlTreatmentsForSplitsWithConfigReturnsControlTreatments() {

        Map<String, SplitResult> result = TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(Arrays.asList("split1", "split2"));

        Assert.assertTrue(result.values().stream().allMatch(splitResult -> Treatments.CONTROL.equals(splitResult.treatment())));
    }
}
