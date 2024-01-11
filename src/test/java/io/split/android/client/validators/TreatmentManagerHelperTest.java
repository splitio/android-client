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

public class TreatmentManagerHelperTest {

    @Test
    public void controlTreatmentsForSplitsValidatesSplitsWhenValidatorAndLoggerAreNotNull() {
        SplitValidator validator = mock(SplitValidator.class);
        ValidationMessageLogger logger = mock(ValidationMessageLogger.class);

        when(validator.validateName("split2")).thenReturn(new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "message"));

        TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(validator, logger, Arrays.asList("split1", "split2"), "tag", SplitResult::treatment);

        verify(validator).validateName("split1");
        verify(validator).validateName("split2");
        verify(logger, atMostOnce()).e("message", "tag");
    }

    @Test
    public void controlTreatmentsForSplitsWithConfigValidatesSplitsWhenValidatorAndLoggerAreNotNull() {
        SplitValidator validator = mock(SplitValidator.class);
        ValidationMessageLogger logger = mock(ValidationMessageLogger.class);

        when(validator.validateName("split2")).thenReturn(new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "message"));

        Map<String, SplitResult> result = TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(validator, logger, Arrays.asList("split1", "split2"), "tag", TreatmentManagerImpl.ResultTransformer::identity);

        verify(validator).validateName("split1");
        verify(validator).validateName("split2");
        verify(logger, atMostOnce()).e("message", "tag");
    }

    @Test
    public void controlTreatmentsForSplitsWithConfigOnlyAddsValueForValidSplits() {
        SplitValidator validator = mock(SplitValidator.class);
        ValidationMessageLogger logger = mock(ValidationMessageLogger.class);

        when(validator.validateName("split2")).thenReturn(new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "message"));

        Map<String, SplitResult> result = TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(validator, logger, Arrays.asList("split1", "split2"), "tag", TreatmentManagerImpl.ResultTransformer::identity);

        verify(validator).validateName("split1");
        verify(validator).validateName("split2");
        verify(logger, atMostOnce()).e("message", "tag");
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.containsKey("split1"));
        Assert.assertFalse(result.containsKey("split2"));
    }

    @Test
    public void controlTreatmentsForSplitsOnlyAddsValuesForValidSplits() {
        SplitValidator validator = mock(SplitValidator.class);
        ValidationMessageLogger logger = mock(ValidationMessageLogger.class);

        when(validator.validateName("split2")).thenReturn(new ValidationErrorInfo(ValidationErrorInfo.ERROR_SOME, "message"));

        Map<String, String> result = TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(validator, logger, Arrays.asList("split1", "split2"), "tag", SplitResult::treatment);

        verify(validator).validateName("split1");
        verify(validator).validateName("split2");
        verify(logger, atMostOnce()).e("message", "tag");
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.containsKey("split1"));
        Assert.assertFalse(result.containsKey("split2"));
    }
}
